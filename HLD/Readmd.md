# The Complete HLD Interview Process

A step-by-step framework for approaching High-Level Design (HLD) interviews.

## 1. Understand the Requirements
**Time: 5 minutes**

Clarify what you are building, who will use it, and what is in scope.

- **Functional requirements:** What must the system do?
- **Non-functional requirements:** How fast, reliable, available, and scalable must it be?
- **Scope:** What are you deliberately not designing?

## 2. Estimate the Scale
**Time: 3 minutes**

Estimate the system's expected load, focusing only on numbers that influence design decisions.

- Number of users and requests per second (RPS).
- Read-to-write ratio.
- Data generated and storage requirements.

## 3. Identify Core Entities and Data
**Time: 3 minutes**

Identify the main objects, their relationships, and the data the system needs to store.

- Define the core entities and their attributes.
- Identify relationships between entities.
- Determine which data needs to be queried or updated efficiently.

## 4. Define the APIs
**Time: 3 minutes**

Define the main endpoints, request and response formats, and supported operations.

- Identify the key API endpoints.
- Define request and response structures.
- Consider pagination, authentication, and idempotency where relevant.

## 5. Draw the High-Level Architecture
**Time: 8 minutes**

Draw the major components required to satisfy the requirements and show how they interact.

- Clients and entry points.
- Application services.
- Databases and caches.
- Message queues and background workers.
- External services, where needed.

## 6. Explain the End-to-End Data Flow
**Time: 5 minutes**

Walk through a key read request and write request.

- Explain how requests travel through the system.
- Identify where data is read, written, and stored.
- Distinguish synchronous operations from asynchronous processing.

## 7. Deep Dive into Critical Decisions
**Time: 12 minutes**

Explore the most important technical decisions, bottlenecks, and trade-offs.

- Database selection and data access patterns.
- Caching strategies.
- Scalability and performance.
- Consistency and concurrency.
- Failure handling and reliability.
- Security and trade-offs.

Focus on the decisions that matter most for the specific problem.

## 8. Summarize and Validate (Optional)
**Time: 3–5 minutes**

Review the design and verify that it satisfies the original requirements.

- Summarize the architecture and key design decisions.
- Highlight the main trade-offs.
- Identify potential bottlenecks and failure scenarios.
- Explain how you would improve the system as traffic grows.

---

## Interview Time Summary

|                  Step                |      Time     |
|--------------------------------------|---------------|
| 1. Understand the Requirements       | 5 min         |
| 2. Estimate the Scale                | 3 min         |
| 3. Identify Core Entities and Data   | 3 min         |
| 4. Define the APIs                   | 3 min         |
| 5. Draw the High-Level Architecture  | 8 min         |
| 6. Explain the End-to-End Data Flow  | 5 min         |
| 7. Deep Dive into Critical Decisions | 12 min        |
| 8. Summarize and Validate (Optional) | 3–5 min       |
| **Total**                            | **42–44 min** |