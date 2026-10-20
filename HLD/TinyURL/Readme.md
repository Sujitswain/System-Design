# TinyURL (URL Shortener)

Design a service that turns a long URL into a short link and redirects visitors to the original URL. The system is read-heavy: redirects are expected to greatly outnumber link creations.

[Open the architecture diagram in Eraser](https://app.eraser.io/workspace/FqGwtCfXOkpkFxeuWgz0)

## 1. Requirements

### Functional requirements

- Create a short link for a valid destination URL.
- Redirect a visitor from a short code to its destination.
- Optionally allow a custom alias and expiration time.
- Return a clear not-found response for an unknown code and an expired response for an expired link.

### Non-functional requirements

- Redirects should be fast and highly available.
- Each short code must identify at most one link.
- The system should scale horizontally and handle many more redirects than writes.
- Link creation should be durable: once creation succeeds, the link should not be lost.

### Out of scope for the core design

- User accounts and link-management dashboards.
- Detailed click analytics and reporting.
- Previewing or fetching the destination URL on the server.

These can be added later without putting analytics or destination fetching on the critical redirect path.

## 2. Assumptions and scale

Use explicit assumptions in an interview and adjust them if the interviewer provides different numbers:

- 100 million redirects per day: about 1,160 requests/second on average.
- Peak traffic is 10 times the average: about 11,600 redirects/second.
- 100,000 new links per day: about 1.2 writes/second on average.
- At 500 bytes per stored link, 1 billion links take about 500 GB of raw primary data, before indexes, replicas, and backups.

This read/write imbalance makes caching important. The numbers are illustrative, not product guarantees.

## 3. API

### Create a link

`POST /api/v1/urls`

Request:

```json
{
  "url": "https://example.com/a/very/long/path",
  "custom_alias": "my-page",
  "expires_at": "2027-01-01T00:00:00Z"
}
```

`custom_alias` and `expires_at` are optional.

- `201 Created`: return the short URL and its expiration, if any.
- `400 Bad Request` or `422 Unprocessable Content`: malformed or unsupported URL.
- `409 Conflict`: requested custom alias is already in use.
- `429 Too Many Requests`: link-creation limit exceeded.

### Redirect

`GET /{code}`

- `302 Found` with a `Location` header when the link exists and is active.
- `404 Not Found` when the code does not exist.
- `410 Gone` when the link has expired or has been disabled.

Use a temporary redirect for the core design so browsers are less likely to retain a permanent mapping. Set an appropriate `Cache-Control` policy: browser or intermediary caching can bypass the service on later visits and make revocation or click measurement less reliable.

## 4. Data model

`UrlMapping`

| Field | Meaning |
|---|---|
| `code` | Short code; primary key and unique |
| `original_url` | Validated destination URL |
| `created_at` | Creation timestamp |
| `expires_at` | Optional expiration timestamp |
| `status` | Active or disabled |
| `owner_id` | Optional creator identifier |

The code's unique constraint is the final authority on uniqueness. Store custom aliases in the same `code` namespace so a custom alias cannot collide with a generated code.

## 5. High-level architecture

```text
Client --> DNS / Load Balancer --> API Gateway
                                    |
                   +----------------+----------------+
                   |                                 |
             POST /api/v1/urls                    GET /{code}
                   |                                 |
                   v                                 v
          Link Creation Service                Redirect Service
                   |                            |          |
                   |                            |          +-- cache miss --> Primary URL Store
                   |                            |
                   |                            +-- lookup / populate --> Distributed Cache
                   v
             Primary URL Store
                   |
                   +----> Read Replicas (if needed)

Optional later: Redirect Service --> Event Queue --> Analytics Workers
```

The redirect service uses cache-aside lookup: it checks the cache, reads the primary store on a miss, then populates the cache. The primary store is the durable source of truth. Read replicas are an option when measured database read load warrants them, not a prerequisite for the first version.

## 6. Link creation flow

1. The client sends the destination URL and optional alias/expiration.
2. The gateway authenticates the caller if accounts are enabled and applies request-size and rate limits.
3. The creation service validates the URL scheme and input. It does not fetch the destination.
4. For a custom alias, attempt to insert it directly; return `409` if the unique constraint reports a conflict.
5. Otherwise generate a random Base62 code (for example, 8 characters) and insert the mapping.
6. If insertion fails specifically because the code already exists, generate another code and retry a small, bounded number of times. Surface other database errors rather than treating them as collisions.
7. Return the short URL only after the mapping has been durably stored.

Random codes avoid a globally coordinated counter. The database unique constraint prevents duplicate mappings even when creation requests race. At very high write volumes, a pre-allocated ID range encoded as Base62 is another option, but introduces ID-allocation and guessability trade-offs.

## 7. Redirect flow

1. A visitor requests `GET /{code}`.
2. The gateway validates the code format and routes the request to a redirect service.
3. The redirect service checks the distributed cache.
4. On a cache hit, it verifies that the cached mapping is active and not past its expiration, then returns `302` with `Location`.
5. On a cache miss, it looks up the code in the URL store. If active, it caches the mapping and redirects; if unknown, it returns `404`; if expired or disabled, it returns `410`.
6. Cache entries must not live beyond the mapping's expiration. Use a shorter TTL where prompt revocation matters.

An optional short-lived negative cache for unknown codes can protect the store from repeated random-code probes. Do not let negative caching prevent a newly created mapping from becoming visible; invalidate the negative entry when creation succeeds, or keep its TTL very short.

## 8. Short-code generation and storage choices

### Code generation

An 8-character Base62 code has `62^8` (about 218 trillion) possible values. Random generation can still collide, so always rely on the database uniqueness constraint and retry only on a confirmed duplicate-key error. Codes are identifiers, not authorization credentials; sensitive or private destinations need separate access control.

### Database

A relational store such as PostgreSQL is a straightforward starting point: the mapping is simple, the unique constraint is useful, and the write rate in the example is modest. A key-value store is also suitable when the main access pattern is exact lookup by code and operational scale justifies it.

Start with a primary store and its indexes. Add replicas or shard the mapping by a stable hash of `code` only when capacity or availability requirements demand it. If sharding, custom-alias writes and generated-code writes must use the same routing rule.

## 9. Caching and consistency

- Use a distributed cache such as Redis for popular code-to-destination lookups.
- Use cache-aside: read cache, load from the store on a miss, then populate cache.
- Cache entries should include status and expiration, and their TTL must be no later than the link's expiration.
- Invalidate or update the cache when a link is disabled, changed, or deleted. Short TTLs limit the time stale redirects can persist if invalidation is delayed.
- Cache misses must be safe under concurrency; duplicate reads are acceptable initially. Add request coalescing only if miss storms become a measured problem.
- A newly created link should be readable immediately from the primary store. If reads use asynchronous replicas, replication lag can cause a just-created code to temporarily appear missing; route the initial read to the primary or otherwise account for that lag.

## 10. Reliability and failure handling

- If the cache is unavailable, fall back to the durable store while protecting it with timeouts, connection limits, and load shedding.
- If the primary store is unavailable and the mapping is not cached, return `503 Service Unavailable`; do not turn infrastructure errors into `404`.
- Keep backups and test restoration. Replication improves availability but is not a backup.
- Expiration is enforced during lookup, so correctness does not depend on a cleanup job. A background job may remove expired rows later.
- If analytics are added, publish click events asynchronously. Analytics failure should not prevent a valid redirect.

## 11. Security and abuse prevention

- Accept only allowed destination schemes such as `http` and `https`; reject `javascript:`, `data:`, and malformed URLs.
- Do not fetch or crawl submitted destinations in the redirect flow. If a future feature fetches URLs for previews or scanning, design explicit SSRF protections.
- Rate-limit creation and custom-alias attempts; consider abuse reporting and link disablement.
- Treat aliases as untrusted input and encode them safely in paths and logs.
- Avoid logging sensitive query parameters from destination URLs.
- A short code is not proof of permission. Do not use unguessable-looking URLs as the only protection for private content.

## 12. Optional analytics extension

If click analytics become a requirement, enqueue a compact event after resolving the mapping and return the redirect without waiting for aggregation. Workers can aggregate counts and coarse dimensions into an analytics store. Define retention, privacy, deduplication, and event-loss requirements separately; exact global click counts are not necessary for the core redirect service.

## 13. Key trade-offs

| Choice | Benefit | Cost or limitation |
|---|---|---|
| Random Base62 code with unique constraint | No global counter; easy to scale writers | Collision retries and codes are not sequential |
| Temporary redirect | Keeps control over future redirects | The browser must contact the short-link service again |
| Distributed cache | Reduces latency and store reads | Stale entries and cache operational cost |
| Store analytics asynchronously | Keeps redirects fast | Analytics are eventually consistent |
| Enforce expiry on read | Expired links stop working without cleanup | Expired rows consume storage until cleanup |

## 14. Further reading

- [Hello Interview: Bitly system design breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/bitly) — used as a study reference; this document presents its own design and trade-offs.
