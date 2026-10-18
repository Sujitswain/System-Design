There are 4 main rate-limiting algorithms you should know for HLD interviews, especially for our distributed Rate Limiter design.

## 1. Fixed Window Counter

[Rate Limiting Algorithms You Should Know | Sergei Bozhko](https://images.openai.com/static-rsc-4/W5UCHNY_486H6Hw1PULvSHzuwgxjVtl56o3IdcjpKUT59FjVridH17ZQb7nFqWsXT4h3QRL9iL9aaaaQc5ChD0gX2AkTyB8uoePrKA3aRHHTbTZuZ0XQvVVT0b0cjqFHAWyTgq8jM_201uGpTQElXswz0klBy0iexppiLebo9bE?purpose=inline)

[sergeibozhko.com](https://sergeibozhko.com/articles/ratelimiting-algorithms/?utm_source=chatgpt.com)

Idea: Count requests within a fixed time window, such as each minute.

Example: Limit = 5 requests/minute.

- Requests 1–5: allowed.
- Request 6: rejected.
- Counter resets when the next minute starts.

```
12:00–12:01 → 5 requests allowed
12:01–12:02 → Counter resets
```

Problem: Boundary burst. A client could make 5 requests at 12:00:59 and another 5 at 12:01:00 — 10 requests in about two seconds.

- Advantage: Simple, fast, low memory.
- Disadvantage: Bursts at window boundaries.

## 2. Sliding Window Log

[A Guide to Rate Limiting Strategies - ByteByteGo Newsletter](https://images.openai.com/static-rsc-4/Fq-h7q89yPYUqi-crbVQng9GYzFyiAUVmEylaZlWhv-zhrqRBr_bxqKQvfUAQw2fxX-TeZAxR9lA9bxT-BhgtgFnwKt1-wNJGgxQnPeYgiFlxT49CNOZUKSl1pJkDG5XZ-PXhETyaH2XulLTNM2rkRocYKPo1B_PKpBpoN7FLLY?purpose=inline)

[blog.bytebytego.com](https://blog.bytebytego.com/p/a-guide-to-rate-limiting-strategies?utm_source=chatgpt.com)

Idea: Store the timestamp of each request and remove timestamps outside the rolling window.

For a limit of 5 requests in the last 60 seconds, allow a request only if fewer than 5 timestamps fall within that interval.

```
Request timestamps: [10:01, 10:12, 10:25, 10:41, 10:53]
New request at 10:55 → Reject
```

All five requests are within the previous 60 seconds.

- Advantage: Accurate rolling-window enforcement.
- Disadvantage: High memory usage and more work per request.

## 3. Sliding Window Counter

[A Guide to Rate Limiting Strategies - ByteByteGo Newsletter](https://images.openai.com/static-rsc-4/n3_c9hNuSPvCZVypfHPuREAIrBvWjT21i8kILJ_2mPsB_uL6r9XailnAYJ5o7iH4wwwM0SIDKjzDmQlcvM7zjGR-AfRryANVx-fAm7AusAMQlvsd1-gPNboAM13UG0V8kdkp2xO8Tu6DW2EQqwX-0KFx6nn16bAWl7UHElcW2gY?purpose=inline)

[blog.bytebytego.com](https://blog.bytebytego.com/p/a-guide-to-rate-limiting-strategies?utm_source=chatgpt.com)

Idea: Combine the previous window's count and the current window's count using a weighted estimate.

Suppose:

- Limit = 100 requests/minute.
- Previous window count = 80.
- Current window count = 30.
- 25% of the current window has elapsed.

Estimated requests in the rolling window:

\\[ 80 \times (1-0.25)+30=90 \\]

The estimate is 90, so the next request can be allowed under the limit of 100.

- Advantage: Lower memory usage than the log and smoother boundary behavior than fixed windows.
- Disadvantage: Approximate rather than exact rolling-window enforcement.

## 4. Token Bucket

[Top Five Rate Limiting Algorithms \~ Key Concepts in System Design | by Kajol Kumari | Feb, 2026 | Medium](https://images.openai.com/static-rsc-4/Yv8iODqtqpwXaAPZpF9JtIceMO1MXqnf3EBa7PNIgE0fwAFcOekjgQSv9Pu2zbcah5Z4OrjEqGMCqAHHiCrLFJL8rwVphXlocx72wZs_cjXJ0Eilaz6bhWt1ltu2byQ2Yh6wWoCkI71aSun9TaUi7jJLAbw70zRKfvT6bhiRsQs?purpose=inline)

[medium.com](https://medium.com/%40kajol_singh/top-five-rate-limiting-algorithms-key-concepts-in-system-design-04b24f822d7f?utm_source=chatgpt.com)

Idea: A bucket holds tokens and refills at a configured rate. Each request consumes one token.

Example:

- Bucket capacity = 10 tokens.
- Refill rate = 2 tokens/second.
- A full bucket permits a burst of 10 requests.
- After the bucket empties, sustained traffic is limited by the refill rate.

* Advantage: Supports controlled bursts and limits sustained traffic.
* Disadvantage: Requires careful atomic state updates in a distributed implementation.

## Quick comparison

| Algorithm              | Memory | Burst handling           | Accuracy                       |
| ---------------------- | ------ | ------------------------ | ------------------------------ |
| Fixed Window           | Low    | Boundary bursts possible | Exact within each fixed window |
| Sliding Window Log     | High   | Strict rolling limit     | Exact                          |
| Sliding Window Counter | Low    | Smoother boundaries      | Approximate                    |
| Token Bucket           | Low    | Allows controlled bursts | Enforces token-bucket policy   |

The Leaky Bucket algorithm is another important rate-limiting algorithm. Let's compare it with Token Bucket, because both are commonly discussed in HLD interviews.

## 1. How Leaky Bucket works

Imagine a bucket with a small hole at the bottom.

[A Guide to Rate Limiting Strategies - ByteByteGo Newsletter](https://images.openai.com/static-rsc-4/vYim6K3_T5CMpKO2xgm33_qVOVptQEvmadN3eWXWUVKZ5QUZbBvI5nfGOcV4sdGdIMnUeQD1ArfgkdkuBcx_yqw2znj6OWdcDRrlDaEZTzb0ZM6Ehx9uVc9_qAKCqywiKjUqCgzi71RjixRXS_e3-yX8_yHKCxeeCS-mOAKtbhY?purpose=inline)

[blog.bytebytego.com](https://blog.bytebytego.com/p/a-guide-to-rate-limiting-strategies?utm_source=chatgpt.com)

Requests enter the bucket; requests are processed at a steady rate.

- Incoming requests enter a bucket, which represents a queue.
- Requests leave the bucket at a constant rate.
- If the bucket becomes full, new requests are rejected.
- The bucket has a maximum capacity, so it cannot grow indefinitely.

### Example

Suppose the system allows processing 5 requests per second, with a bucket capacity of 10 requests.

1. Twenty requests arrive almost simultaneously.
2. The bucket can hold only 10 requests, so 10 are rejected.
3. The accepted requests are processed steadily at 5 requests per second.
4. If more requests arrive while the bucket is full, they are rejected.

The key idea: Leaky Bucket smooths incoming traffic into a steady outgoing flow.

## 2. Leaky Bucket vs. Token Bucket

### Leaky Bucket

Controls the rate at which requests leave the bucket.

- Processes requests at a steady rate.
- Excess requests wait in a queue, if queueing is used.
- Rejects requests when the queue is full.
- Useful for smoothing traffic.

### Token Bucket

Controls how quickly requests are allowed to consume tokens.

- Allows short bursts when tokens have accumulated.
- Refills tokens at a configured rate.
- Rejects requests when insufficient tokens remain.
- Useful for API rate limiting that permits controlled bursts.

## 3. Which one should we choose for our Rate Limiter HLD?

For our API Gateway Rate Limiter, I'd choose Token Bucket as the default.

For example, an API allows 100 requests per minute but should tolerate a short burst of requests. Token Bucket can allow that burst, while Leaky Bucket is better suited when we need to smooth traffic into a more consistent processing rate.

One important distinction: Leaky Bucket can refer to a queue-based traffic shaper or a request-rejection rate limiter. In a queue-based design, requests wait; in a rejection-based design, excess requests are dropped.

For your interview, remember these four algorithms plus Leaky Bucket as an additional one:

1. Fixed Window Counter
2. Sliding Window Log
3. Sliding Window Counter
4. Token Bucket
5. Leaky Bucket

Token Bucket and Leaky Bucket are related ideas, but they solve slightly different traffic-control problems.