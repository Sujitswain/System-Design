
# Non-Functional Requirements (NFRs) — HLD Interview Guide

## 1. What to Consider in Every HLD

Before designing the architecture, identify the important non-functional requirements (NFRs).

- Scalability
- Capacity and Throughput
- Performance and Latency
- Availability
- Reliability
- Fault Tolerance
- Durability
- Consistency and CAP Theorem
- Security
- Observability
- Disaster Recovery
- Cost Efficiency

**Important:** Do not blindly include every NFR in every design. Identify which ones matter most for the system and define measurable targets wherever possible.

---

## 2. Scalability vs. Throughput vs. Performance

These concepts are related, but they are NOT the same.

### Scalability

**Question:** Can the system handle increasing traffic, users, or data?

Consider:
- Horizontal scaling — add more instances.
- Vertical scaling — increase CPU or memory.
- Load balancing across instances.
- Database partitioning and sharding.
- Caching frequently accessed data.
- Asynchronous processing using queues.
- Avoiding bottlenecks and hot partitions.

### Throughput

**Question:** How many requests or operations can the system process per second?

Consider:
- Requests per second (RPS).
- Transactions per second (TPS).
- Messages processed per second.
- Database throughput.
- Concurrent requests.
- Peak traffic and traffic spikes.
- Downstream service capacity.

### Performance

**Question:** How quickly and efficiently does the system respond?

Consider:
- Response time.
- p50, p95, and p99 latency.
- Database query performance.
- Cache hit ratio.
- Network latency.
- CPU and memory utilization.

### Capacity Estimation

Estimate:
- Average and peak requests per second.
- Read/write ratio.
- Concurrent users and requests.
- Storage required per day and per year.
- Data retention period.
- Expected growth.
- Additional headroom for spikes and failures.

Example:

If a system receives 86.4 million requests per day:

Average RPS = 86,400,000 / 86,400 = 1,000

If peak traffic is 5 times the average:

Peak RPS = 1,000 × 5 = 5,000

The system should be tested against the expected peak, with suitable capacity headroom.

---

## 3. Availability

**Question:** Can users access the service when they need it?

Consider:
- Multiple application instances.
- Load balancers.
- Health checks.
- Multiple availability zones.
- Database replicas.
- Automatic failover.
- Reducing single points of failure.
- Monitoring and alerting.

### What to Do When a Component Fails

1. Detect the failure using health checks and monitoring.
2. Stop routing traffic to unhealthy instances.
3. Route requests to healthy instances or replicas.
4. Apply timeouts to prevent requests from hanging indefinitely.
5. Use bounded retries with exponential backoff and jitter where appropriate.
6. Recover or replace the failed component.
7. Verify that the service has recovered.

### Availability Targets

| Availability | Approximate Downtime per Year |
|---|---|
| 99% | 3 days 15 hours 36 minutes |
| 99.9% | 8 hours 46 minutes |
| 99.99% | 52 minutes 34 seconds |
| 99.999% | 5 minutes 15 seconds |

Higher availability usually requires more redundancy (more duplicates), operational complexity, and cost.

---

## 4. Reliability

**Question:** Does the system perform its intended function correctly and consistently over time?

Consider:
- Input validation.
- Error handling.
- Idempotent operations.
- Safe retries.
- Transaction management.
- Monitoring and alerts.
- Automated tests.
- Data integrity checks.

Example:

A payment API must not charge a customer twice when a client retries the same request.

Solution:
- Use an idempotency key.
- Store the outcome of the original operation.
- Return the original result for duplicate requests.

---

## 5. Fault Tolerance

**Question:** Can the system continue operating when a component fails?

Consider:
- Redundant application instances.
- Replication.
- Automatic failover.
- Timeouts.
- Circuit breakers.
- Bulkheads.
- Graceful degradation.
- Dead-letter queues for poison messages.
- Recovery procedures.

### Failure Handling Checklist

| Failure | Possible Response |
|---|---|
| Application instance crashes | Route traffic to healthy instances and replace the failed instance |
| Downstream service is slow | Apply timeouts and prevent resource exhaustion |
| Repeated downstream failures | Use a circuit breaker |
| Temporary network failure | Retry safely with bounded exponential backoff and jitter |
| Database is unavailable | Return a controlled error, use a safe fallback, or queue work where appropriate |
| Message processing fails | Retry safely, track attempts, and use a dead-letter queue where appropriate |
| Cache or Redis fails | Decide whether to use a fallback, fail open, or fail closed |
| Entire availability zone fails | Route traffic to another healthy zone |
| Region fails | Activate disaster recovery according to the recovery objectives |

**Important:** Retrying every failure is not always correct. Retries can increase load and create retry storms. Use them only when appropriate, with limits and timeouts.

---

## 6. Durability

**Question:** Once data is successfully committed, will it survive the specified failures?

Consider:
- Persistent storage.
- Database replication.
- Write-ahead logs.
- Backups.
- Backup restoration testing.
- Data retention policies.
- Disaster recovery.
- Acknowledgement guarantees.

Example:

After a payment is confirmed, its transaction record should not disappear because an application instance crashes.

Replication protects against some failures, but it does not replace backups. Replication can also copy accidental deletions or corrupted data.

---

## 7. Latency

**Question:** How quickly must a request complete?

Consider:
- p50, p95, and p99 latency.
- Database indexing.
- Caching.
- Network calls.
- Number of synchronous dependencies.
- Timeouts and deadlines.
- Asynchronous processing for work that need not finish during the request.

Example NFR:

95% of read API requests should complete within 200 ms at the expected peak load.

Avoid using only average latency because a good average can hide very slow requests.

---

## 8. Consistency

**Question:** When a write occurs, when should other readers see the updated data?

### Strong Consistency

Reads observe the latest committed value according to the system's consistency guarantee.

Useful for:
- Bank account balances.
- Payment state transitions.
- Inventory reservations.
- Critical financial operations.

### Eventual Consistency

Replicas may temporarily return different values but converge when updates propagate successfully.

Useful for:
- Analytics.
- Activity feeds.
- Search indexes.
- Some counters and dashboards.

Also consider:
- Read-after-write guarantees.
- Event ordering.
- Transaction boundaries.
- Duplicate message processing.
- Conflict resolution.

Choose consistency according to the operation's correctness requirements.

---

## 9. CAP Theorem

CAP describes the trade-off in a distributed system when a network partition occurs.

- **C — Consistency:** Reads behave as though they observe a single up-to-date value.
- **A — Availability:** Every request to a non-failing node receives a response, though the response might be stale.
- **P — Partition Tolerance:** The system continues operating despite communication failures between parts of the system.

During a network partition, a distributed system cannot guarantee both CAP consistency and CAP availability for every request.

### What Should We Choose?

**Prefer consistency:**
- Reject or delay requests when a safe, up-to-date result cannot be guaranteed.
- Appropriate for critical balance updates and certain financial operations.

**Prefer availability:**
- Continue serving requests, potentially with stale or conflicting data.
- Appropriate where temporary inconsistency is acceptable.

CAP does not mean a system must permanently choose only consistency or availability under normal conditions.

---

## 10. Disaster Recovery

Consider what happens when a major component, availability zone, or region becomes unavailable.

### RTO — Recovery Time Objective

How long can the service remain unavailable before it must recover?

Example: RTO = 30 minutes.

### RPO — Recovery Point Objective

How much recent data loss is acceptable, expressed as time?

Example: RPO = 5 minutes.

Consider:
- Automated recovery.
- Backups and restoration.
- Replication strategy.
- Standby infrastructure.
- Regional failover.
- Recovery runbooks.
- Periodic disaster recovery testing.

---

## 11. Security

Consider:
- Authentication — who is the caller?
- Authorization — what is the caller allowed to do?
- Encryption in transit and at rest.
- Secrets management.
- Input validation.
- Rate limiting and abuse prevention.
- Least-privilege access.
- Audit logging.
- Personal data protection.
- Data retention and compliance.

Security requirements should influence the architecture from the beginning.

---

## 12. Observability

**Question:** How will we know the system is healthy and diagnose failures?

Consider:
- Metrics.
- Structured logs.
- Distributed tracing.
- Request and correlation IDs.
- Error rates.
- Latency percentiles.
- Resource saturation.
- Queue lag.
- Cache hit ratio.
- Alerts and dashboards.

Monitor user-facing symptoms as well as individual component health.

---

## 13. Cost Efficiency and Maintainability

Consider:
- Compute and storage costs.
- Network transfer costs.
- Replication and redundancy costs.
- Managed service costs.
- Operational complexity.
- Ease of debugging and maintenance.
- Deployment and rollback strategies.
- Future extensibility.

Do not introduce distributed systems complexity without a clear requirement.

---

## 14. Reusable NFR Checklist for HLD Interviews

Use this checklist before drawing the architecture.

### Scale and Performance
- [ ] Average and peak RPS/TPS
- [ ] Concurrent users and requests
- [ ] Read/write ratio
- [ ] Data volume and growth
- [ ] Latency targets (p95/p99)
- [ ] Throughput and capacity
- [ ] Scaling strategy and headroom

### Availability and Recovery
- [ ] Availability target
- [ ] Single point of failure
- [ ] Instance and availability-zone failures
- [ ] Failover strategy
- [ ] Timeout and retry policies
- [ ] Circuit breakers and graceful degradation
- [ ] RTO and RPO

### Data and Correctness
- [ ] Strong or eventual consistency
- [ ] Transaction requirements
- [ ] Durability guarantees
- [ ] Replication and backups
- [ ] Idempotency and duplicate handling
- [ ] Ordering requirements
- [ ] Data retention

### Security and Operations
- [ ] Authentication and authorization
- [ ] Encryption and privacy
- [ ] Compliance requirements
- [ ] Metrics, logs, and traces
- [ ] Alerting and incident recovery
- [ ] Cost constraints
- [ ] Maintainability

---

## 15. Example: Rate Limiter

| NFR | Design Consideration |
|---|---|
| Low latency | Minimize the overhead added to each API request |
| High throughput | Support the required number of rate-limit checks per second |
| Scalability | Scale gateway instances and limiter storage as traffic grows |
| Availability | Decide whether Redis failure should fail open or fail closed |
| Consistency | Decide whether the limit must be enforced globally or small temporary overshoot is acceptable |
| Fault tolerance | Handle timeouts, Redis failures, and network issues |
| Durability | Decide whether counters can be lost after restart |
| Security | Derive client identity from trusted authentication or proxy information |
| Observability | Track allowed requests, rejected requests, errors, and latency |

### Fail Open vs. Fail Closed

**Fail open:** If the limiter cannot make a decision, allow the request.

- Preserves application availability.
- May allow excessive traffic or abuse.

**Fail closed:** If the limiter cannot make a decision, reject the request.

- Protects restricted resources.
- May reject legitimate traffic during a failure.

Choose based on the risk of allowing an unchecked request versus denying a legitimate request.

---

## Final Rule

NFRs are constraints that drive architecture decisions.

Do not merely list scalability, availability, durability, and fault tolerance. Explain:

1. What the system needs.
2. How the requirement will be measured.
3. What design choice satisfies it.
4. What happens when a component fails.
5. What trade-offs the design introduces.
