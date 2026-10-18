# Distributed Rate Limiter

A rate limiter controls how frequently a client can access a service. It protects services from accidental overload and abusive traffic while allowing normal requests through.

[View the architecture diagram in Eraser](https://app.eraser.io/workspace/FqGwtCfXOkpkFxeuWgz0?origin=share)

## 1. Requirements

### Functional

- Apply configurable limits by user, API key, IP address, route, or a combination.
- Allow or reject each request before it reaches the protected service.
- Return the limit and retry information to clients.
- Support different policies for different routes or client tiers.

### Non-functional

- Make a decision with low added latency.
- Enforce limits across multiple gateway instances.
- Remain horizontally scalable and avoid a single application-server bottleneck.
- Degrade predictably if the shared rate-limit store is unavailable.

### Out of scope

- A user-facing policy management interface.
- Billing and long-term usage analytics.
- Blocking malicious clients beyond the configured rate policy.

## 2. Example policy and semantics

Example: allow an authenticated user an average of **100 requests per minute**, with a **burst capacity of 100**.

This design uses a **token bucket**. The bucket holds up to 100 tokens and refills at 100 tokens per minute (about 1.67 tokens per second). Each request costs one token. A full bucket permits a burst of 100 requests; after it is empty, requests are allowed as tokens become available.

This is not a fixed one-minute window: tokens do not all reset at the minute boundary. If a strict count in every rolling 60-second period is required, choose a sliding-window algorithm instead.

## 3. High-level architecture

```text
Client
  |
  v
Load Balancer
  |
  v
API Gateway / Service Middleware
  |                         \
  | rate-limit decision      \ allowed
  v                            v
Redis (shared state)       Application Service
                               |
                               v
                         Database / Services

Policy Configuration ---> Gateway policy cache
```

- **Gateway or middleware:** authenticates the request, identifies the limiter key, loads the policy, and checks the limit before forwarding.
- **Redis:** stores short-lived bucket state shared by all gateway instances.
- **Policy configuration:** supplies limits and failure behavior. Gateways cache the last known good policy so a configuration-store outage does not erase active policies.
- **Application service:** receives only requests allowed by the limiter.

The request path is synchronous: the gateway must receive the Redis decision before forwarding the request.

## 4. Request flow

1. The gateway authenticates the request and determines its trusted identity and route.
2. It selects the matching policy, for example `user + route + policy`.
3. It runs one atomic Redis operation to refill the bucket and try to consume one token.
4. If a token is available, Redis updates the bucket and the gateway forwards the request.
5. If no token is available, the gateway returns **HTTP 429 Too Many Requests** without calling the application service.
6. The gateway includes rate-limit headers when it has a valid decision.

## 5. Data model and atomic decision

Store one bucket per limiter key, for example:

```text
rl:{hashed-subject}:{route}:{policy-version}
  tokens          current token count
  last_refill_ms  last refill time
```

The subject should come from verified authentication data. For anonymous traffic, use the client IP only after the gateway has safely resolved trusted proxy headers. Hash or HMAC identifiers in Redis keys if exposing raw identifiers is undesirable. A policy can use a composite identity, such as `user + route`, to give each user a separate route-specific bucket.

The gateway calls a Redis Lua script (or an equivalent atomic Redis operation) that:

1. Reads the bucket and current Redis server time.
2. Refills tokens by `elapsed time × refill rate`, capped at bucket capacity.
3. Consumes one token if at least one is available.
4. Writes the updated token count and timestamp.
5. Sets an expiry so inactive buckets are eventually removed.
6. Returns whether the request was allowed, remaining tokens, and the time until another token is available.

The check and update must be atomic. A separate read followed by a write can allow concurrent gateway requests to consume the same token. Use Redis server time to avoid decisions depending on clock differences between gateway machines.

Keep all state needed for one decision under one Redis key. This works with Redis Cluster because a single-key script is routed to one hash slot.

## 6. Internal decision and HTTP response

The limiter returns an internal decision similar to:

```text
allowed: boolean
limit: integer
remaining: integer
retry_after_seconds: integer, when denied
```

- **Allowed:** forward the request and include `RateLimit-Limit`, `RateLimit-Remaining`, and `RateLimit-Reset` when supported by the API.
- **Denied:** return `429 Too Many Requests` with `Retry-After` indicating when a token is expected to become available.
- Do not return `429` for a Redis or internal error; that means the service could not determine whether the quota was exceeded.

## 7. Scaling and consistency

- Scale gateways horizontally; they remain stateless apart from their policy cache.
- Use a Redis primary/cluster sized and load-tested for the expected decision rate. Monitor latency, CPU, memory, evictions, and rejected/failed commands.
- Redis is a hot-path dependency. Set a short timeout and connection pooling; do not silently retry a timed-out decision, since the first operation may have succeeded.
- A single popular identity produces a hot key. A single atomic bucket cannot be split across shards without changing its semantics.
- In a multi-region deployment, use regional buckets for lower latency and availability, understanding that a client may receive a separate allowance in each region. A strict global quota needs coordinated state and adds cross-region latency and availability trade-offs.
- Redis replication/failover can lose very recent bucket updates. This design provides practical rate enforcement, not a durable financial ledger or an exact global quota.

## 8. Failure behavior

Make failure behavior configurable by policy:

- **Fail closed** for expensive, sensitive, or abuse-prone endpoints. If Redis is unavailable, return `503 Service Unavailable` (not `429`) and alert.
- **Fail open** for low-risk endpoints where availability is more important. Forward the request, record the limiter failure, and accept that the configured limit is temporarily unenforced.
- Do not use per-gateway local counters as if they preserve a shared limit. They can multiply the effective allowance by the number of gateway instances. A local emergency limiter may be added only as a clearly documented best-effort safeguard.

Retain the last known good policy on configuration refresh errors. Set Redis timeouts and circuit-breaker behavior so a Redis outage does not exhaust gateway connections or threads.

## 9. Security and observability

- Derive user and API-key identities from validated credentials, not arbitrary client-supplied headers.
- Trust forwarded IP headers only from known proxies; otherwise clients can spoof their limiter identity.
- A single decision in this design updates one bucket atomically. If both per-user and per-IP caps must be enforced, use a store and key-partitioning strategy that can atomically check and consume all applicable buckets; separate checks can consume one quota even if another check rejects the request.
- Track allowed, rejected, and failed decisions; Redis latency and errors; policy-cache age; and top-level saturation signals.
- Avoid logging raw tokens, API keys, or sensitive user identifiers.

## 10. Algorithm choice

| Algorithm | Benefits | Trade-offs |
|---|---|---|
| Fixed window | Simple and inexpensive | Allows bursts at window boundaries |
| Sliding window counter | Smooths boundary bursts with modest state | Approximate near the boundary |
| Sliding window log | Accurate rolling-window count | More memory and work per request |
| Token bucket | Smooth refill and configurable bursts | Does not enforce a strict count in every rolling window |
| Leaky bucket | Smooth, steady output rate | Can add queueing delay or require dropping requests |

Token bucket is selected here because it is efficient for a synchronous gateway check and supports controlled bursts.

## 11. Key trade-offs

- **Shared Redis vs. local counters:** shared state enforces a common limit across gateways but adds network latency and a dependency; local counters are faster but do not enforce a global per-client quota.
- **Availability vs. enforcement during an outage:** fail-open preserves service availability but may exceed limits; fail-closed preserves protection but may reject healthy traffic.
- **Regional vs. global limits:** regional state is faster and more available; strict global coordination is more expensive and less available during network partitions.

## 12. Validation checklist

- Concurrent requests cannot consume more tokens than the bucket contains.
- Tokens refill at the configured rate and never exceed bucket capacity.
- A denied request receives a useful `Retry-After` value.
- Different users, routes, and policy versions do not share bucket state accidentally.
- Redis outage behavior matches each policy's fail-open/fail-closed setting.
- Load tests verify decision latency and Redis capacity at expected peak traffic.
