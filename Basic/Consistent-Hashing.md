# Consistent Hashing — Complete Learning Guide

## 1. Start with the Basics: Why Do We Need a Load Balancer?

Imagine we are building an application with a backend API.

Initially, we have only one server:

```text
                 Client
                    |
                    v
              +-----------+
              | Server A  |
              +-----------+
```

Every request goes to Server A.

As the number of users increases, Server A may not be able to handle all requests. It can become overloaded, increasing response times and potentially causing failures.

The solution is to run multiple backend servers.

```text
                 Clients
                    |
                    v
              +-------------+
              | Load Balancer|
              +-------------+
                /    |    \
               v     v     v
          +--------+ +--------+ +--------+
          | Server A| | Server B| | Server C|
          +--------+ +--------+ +--------+
```

### What does a load balancer do?

A load balancer receives incoming requests and decides which backend server should handle each request.

Its goals include:

- Distributing requests across multiple servers.
- Avoiding overloaded servers.
- Improving availability and scalability.
- Routing requests only to healthy servers, depending on its configuration.

### Common load-balancing algorithms

| Algorithm | How it works |
|---|---|
| Round Robin | Sends requests to servers in a repeating order. |
| Weighted Round Robin | Sends more requests to servers with higher assigned weights. |
| Least Connections | Prefers the server with fewer active connections. |
| Random | Selects a server randomly. |
| IP Hash | Uses the client's IP address to choose a server. |
| Consistent Hashing | Maps a key to a position on a hash ring and selects the next available server. |

**Important:** Consistent hashing is not required for every load balancer. It is especially useful when we want requests with the same key to keep reaching the same server, while minimizing remapping when servers change.

---

## 2. Why Is Normal Load Balancing Sometimes a Problem?

Let's consider a backend application with three servers:

```text
Server A
Server B
Server C
```

Suppose our application stores session information in the memory of each server.

For example:

```text
Server A:
    User 101 -> Session Data
    User 102 -> Session Data

Server B:
    User 201 -> Session Data

Server C:
    User 301 -> Session Data
```

A user logs in, and the load balancer routes the request to Server A.

Server A creates the session in its local memory.

```text
User 101 -> Load Balancer -> Server A
```

Now the same user sends another request.

If the load balancer sends it to Server B, Server B might not have that user's session.

```text
First request:
User 101 -> Server A

Second request:
User 101 -> Server B
```

This can cause session lookup failures unless the application uses shared session storage, stateless authentication, or another session-management mechanism.

One possible solution is **sticky sessions**, where the load balancer attempts to route a client back to the same server.

Another solution is consistent hashing, where a stable key—such as a user ID—determines the server.

```text
User 101 -> Consistent Hashing -> Server A
User 101 -> Consistent Hashing -> Server A
User 101 -> Consistent Hashing -> Server A
```

As long as the server membership and hashing configuration remain unchanged, the same key maps to the same server.

This is also useful for:

- Distributed caches.
- Sharding data across database nodes.
- Routing requests to services that maintain local state.
- Partitioning workloads across workers.

**Note:** Consistent hashing does not automatically preserve sessions if a server fails, nor does it replicate data. Those require additional mechanisms.

---

## 3. Why Not Use a Simple Hash Function?

Suppose we have three servers:

```text
Server A
Server B
Server C
```

We can calculate a hash from the user's ID and select a server using the modulo operator.

```java
int serverIndex = Math.abs(userId.hashCode()) % 3;
```

Conceptually:

```text
hash(userId) % numberOfServers
```

For example:

```text
User 101 -> hash % 3 = 0 -> Server A
User 102 -> hash % 3 = 1 -> Server B
User 103 -> hash % 3 = 2 -> Server C
User 104 -> hash % 3 = 0 -> Server A
```

This is simple and can distribute keys reasonably well.

However, consider what happens when we add Server D.

Before adding the server:

```text
hash(key) % 3
```

After adding the server:

```text
hash(key) % 4
```

The divisor changes, so many keys now produce different results.

For example, the same key may map to Server A before the change and Server C afterward.

If millions of keys are distributed across the servers, many of them can move to different servers.

For a distributed cache, this can mean:

- Cache misses.
- Increased requests to the database.
- A temporary spike in latency.
- Significant data movement if data must be relocated.

The same issue occurs when a server is removed and the divisor changes again.

We need a way to add or remove servers without remapping most of the keys.

That is the problem consistent hashing solves.

---

## 4. What Is Consistent Hashing?

**Consistent hashing is a hashing technique that distributes keys and servers across a circular hash space so that adding or removing a server typically remaps only a fraction of the keys.**

Instead of calculating:

```text
hash(key) % numberOfServers
```

we calculate a hash for each key and each server.

We place both on the same logical hash space, called a **hash ring**.

The ring is circular, meaning that after the largest hash position, we wrap around to the smallest position.

For illustration, imagine hash positions from 0 to 99.

```text
                 25
             .--------.
          15/          \35
           /            \
         5|              |45
          |              |
         95|              |55
           \            /
          85\          /65
             '--------'
                 75
```

The positions in this diagram are illustrative. Real implementations typically use a much larger hash space.

We can put servers on this ring:

```text
Hash(Server A) = 20
Hash(Server B) = 50
Hash(Server C) = 80
```

We also hash each incoming key:

```text
Hash(User 101) = 25
Hash(User 102) = 65
Hash(User 103) = 90
```

Now we need one simple rule to select a server.

---

## 5. The Most Important Rule: Clockwise Lookup

In a standard consistent-hashing ring, we place a key at its hash position and move **clockwise until we find the first server position**.

If we reach the end of the ring, we wrap around to the beginning.

For example, our ring contains:

```text
Server A = 20
Server B = 50
Server C = 80
```

Consider these keys:

```text
User 101 -> hash 25
User 102 -> hash 65
User 103 -> hash 90
```

The mapping is:

| Key hash | First server clockwise | Selected server |
|---:|---:|---|
| 25 | 50 | Server B |
| 65 | 80 | Server C |
| 90 | Wrap around to 20 | Server A |

The last row is particularly important.

When the key hash is 90, there is no server position between 90 and 99. We wrap around to the beginning of the ring and reach Server A at position 20.

### A simplified Java-style lookup

```java
int keyHash = hash(key);

// Find the first server position >= keyHash.
// If none exists, wrap around to the first server position.
Server server = findFirstServerClockwise(keyHash);
```

The exact data structure used to find the next server depends on the implementation. A sorted map is a common choice.

**Remember this rule: hash the key, move clockwise, and select the first server.**

---

## 6. What Happens When We Add a Server?

This is where consistent hashing becomes useful.

Initially, our ring has:

```text
Server A = 20
Server B = 50
Server C = 80
```

Consider a key with hash 35.

```text
Key hash 35 -> clockwise -> Server B at 50
```

Now add Server D at position 40.

```text
Server A = 20
Server D = 40
Server B = 50
Server C = 80
```

The key at position 35 now maps to Server D.

```text
Before:
Key 35 -> Server B

After:
Key 35 -> Server D
```

Other keys may remain unchanged.

For example:

```text
Key 10 -> Server A before and after
Key 60 -> Server C before and after
```

Why?

Because adding Server D only changes the clockwise destination for keys that fall in the interval immediately preceding Server D, from the previous server position up to the new server position.

In this example, the affected interval is:

```text
(20, 40] -> Server D
```

Keys in that interval previously went to Server B. They now go to Server D.

The remaining keys continue to map to the same servers.

### The benefit

With modulo-based hashing, adding a server can remap a large fraction of keys.

With consistent hashing, adding a server typically remaps only the keys that now belong to that server's ring interval.

This minimizes unnecessary reassignment.

---

## 7. What Happens When We Remove a Server?

Start with:

```text
Server A = 20
Server D = 40
Server B = 50
Server C = 80
```

Suppose Server D fails or is removed from the ring.

Before removal:

```text
Key 35 -> Server D
```

After removal:

```text
Key 35 -> Server B
```

Why Server B?

Because Server B is now the first server clockwise from position 35.

Other keys that previously mapped to Server D also move to the next available server clockwise.

Keys that were already mapped to Server A, Server B, or Server C generally remain mapped to those servers.

### What if Server B is removed?

Suppose Server B at position 50 is removed.

Keys in the interval:

```text
(40, 50]
```

previously mapped to Server B. They now map to Server C at position 80.

Keys outside that interval are generally unaffected.

### What happens to the data on the removed server?

Consistent hashing changes the mapping. It does not copy or recover the data automatically.

For example, if Server D stores cached objects and then fails:

1. The ring removes Server D.
2. The affected keys map to their next available servers.
3. Requests for those keys may encounter cache misses.
4. The application can fetch the missing data from the database and repopulate the cache.

For durable database sharding, data migration, replication, and recovery must be designed separately.

---

## 8. A Common Problem: Uneven Distribution

So far, we have placed each server at exactly one position on the ring.

That is a useful starting point, but it has a limitation.

Suppose the ring positions are:

```text
Server A = 10
Server B = 15
Server C = 90
```

The clockwise intervals are very uneven.

```text
Server A owns keys in (90, 10]  -> 16 positions
Server B owns keys in (10, 15]  -> 5 positions
Server C owns keys in (15, 90]  -> 75 positions
```

Here, Server C owns 75% of the illustrative hash space.

Server C may receive many more keys than the other servers.

This is called **uneven distribution**, or load imbalance.

Real hash spaces are much larger, and actual request traffic can be skewed even when hash positions are evenly distributed. A ring with one position per server can still produce significant imbalance.

We need to improve the distribution.

The solution is virtual nodes.

---

## 9. What Are Virtual Nodes?

A **virtual node**, often called a vnode, is a logical position on the hash ring that represents a physical server.

Instead of placing each physical server on the ring only once, we create multiple logical positions for it.

For example:

```text
Physical Server A:
    A-1
    A-2
    A-3
    A-4

Physical Server B:
    B-1
    B-2
    B-3
    B-4

Physical Server C:
    C-1
    C-2
    C-3
    C-4
```

Each virtual node receives its own position on the ring.

```text
hash("A-1") -> 12
hash("A-2") -> 36
hash("A-3") -> 61
hash("A-4") -> 87

hash("B-1") -> 20
hash("B-2") -> 44
hash("B-3") -> 72
hash("B-4") -> 96

hash("C-1") -> 5
hash("C-2") -> 31
hash("C-3") -> 54
hash("C-4") -> 79
```

These positions are illustrative rather than the result of a real hash calculation.

The ring now has 12 positions rather than just 3.

When we hash a key, we move clockwise to the next virtual node and then route the request to the physical server that owns that virtual node.

For example:

```text
Key hash = 40

Next virtual node clockwise = B-2 at 44

B-2 belongs to physical Server B

Therefore:
Key -> Server B
```

Another example:

```text
Key hash = 63

Next virtual node clockwise = B-3 at 72

B-3 belongs to physical Server B

Therefore:
Key -> Server B
```

Notice that multiple virtual nodes can belong to the same physical server.

### Why are virtual nodes useful?

They help in several ways:

1. **Better distribution:** More positions generally reduce the chance that one server owns an unusually large ring interval.
2. **More balanced reassignment:** When a server is added, it can take over several smaller intervals instead of one large interval.
3. **Heterogeneous server capacity:** A powerful server can receive more virtual nodes than a smaller server.
4. **Smoother scaling:** Adding or removing a physical server affects multiple smaller portions of the ring.

Virtual nodes improve expected distribution; they do not guarantee equal request traffic.

---

## 10. Multiple Hashes for the Same Server

This is an important interview topic.

Suppose we have three physical servers:

```text
Server A
Server B
Server C
```

We can hash each server name once:

```text
hash("Server A") -> one ring position
hash("Server B") -> one ring position
hash("Server C") -> one ring position
```

But one position per server may produce uneven distribution.

Instead, we create several virtual-node identifiers for each physical server.

For example:

```text
Server A:
    hash("Server A#1")
    hash("Server A#2")
    hash("Server A#3")
    ...
    hash("Server A#100")

Server B:
    hash("Server B#1")
    hash("Server B#2")
    hash("Server B#3")
    ...
    hash("Server B#100")

Server C:
    hash("Server C#1")
    hash("Server C#2")
    hash("Server C#3")
    ...
    hash("Server C#100")
```

If we use 100 virtual nodes per physical server, the ring contains 300 virtual-node positions.

The suffixes ensure that each virtual node has a distinct input to the hash function.

For example:

```java
hash("Server A#1")
hash("Server A#2")
hash("Server A#3")
```

These produce different hash positions in a typical well-behaved hash function.

We maintain a mapping between every virtual node and its physical server:

```text
Virtual node       Physical server

Server A#1    ->   Server A
Server A#2    ->   Server A
Server A#3    ->   Server A

Server B#1    ->   Server B
Server B#2    ->   Server B
Server B#3    ->   Server B
```

When a key selects `Server B#2`, the request goes to physical Server B.

**The key is not being replicated onto every virtual node.** Virtual nodes are positions used for routing. They do not represent separate physical machines.

### How many virtual nodes should we use?

There is no universal correct number.

Implementations may use dozens, hundreds, or more virtual nodes per physical server, depending on the system and its performance requirements.

More virtual nodes can improve distribution, but they also increase:

- Ring metadata.
- Memory usage.
- Server membership update work.
- The number of positions managed by the implementation.

The appropriate number should be selected through the implementation's design and testing.

---

## 11. Adding a Physical Server with Virtual Nodes

Suppose the ring currently has:

```text
Server A -> 100 virtual nodes
Server B -> 100 virtual nodes
Server C -> 100 virtual nodes
```

Now we add Server D.

We generate its virtual nodes:

```text
hash("Server D#1")
hash("Server D#2")
...
hash("Server D#100")
```

We insert these positions into the ring.

For each newly inserted virtual node, a portion of the keys that previously belonged to its clockwise successor may now map to Server D.

For example:

```text
Before:

Key X -> Server B
Key Y -> Server C
Key Z -> Server A
```

After adding Server D:

```text
Key X -> Server D
Key Y -> Server C
Key Z -> Server A
```

Only keys in intervals taken over by Server D need to change their destinations.

Because Server D has many virtual nodes spread across the ring, it receives many smaller intervals.

This typically provides better distribution than assigning it a single ring position.

### Does every key get reassigned?

No.

A correctly implemented consistent-hashing ring changes the destination only for keys whose clockwise successor changes.

With reasonably distributed virtual nodes, adding one equally weighted server to a cluster of N equally weighted servers typically moves approximately 1/(N+1) of the keys to the new server.

For example, adding a fourth equally weighted server to a three-server cluster would typically move around 25% of keys to the new server—not every key.

This is an expected proportion, not a guarantee for every particular ring or workload.

---

## 12. Removing a Physical Server with Virtual Nodes

Suppose Server B has 100 virtual nodes.

When Server B is removed:

1. Remove all 100 of Server B's virtual-node positions from the ring.
2. The affected ring intervals now belong to the next surviving virtual nodes clockwise.
3. Requests for those keys are routed to the physical servers owning those virtual nodes.
4. Other key mappings remain unchanged unless their successor positions have changed.

For example:

```text
Before:

Key X -> Server B
Key Y -> Server A
Key Z -> Server C
```

After removing Server B:

```text
Key X -> Server C
Key Y -> Server A
Key Z -> Server C
```

The exact destination for Key X depends on the next surviving virtual node clockwise from its hash position.

### Important distinction

Consistent hashing helps minimize the number of keys that change owners.

It does not guarantee that the remaining servers have enough CPU, memory, network capacity, or storage to absorb the reassigned workload.

A production system must also monitor capacity and handle server health, replication, and failures appropriately.

---

## 13. Consistent Hashing in a Load Balancer

Now let's connect everything back to the load balancer.

Imagine an API service with these backend instances:

```text
                    Clients
                       |
                       v
                +---------------+
                | Load Balancer |
                +---------------+
                       |
                       v
               Consistent Hashing
                       |
            +----------+----------+
            |          |          |
            v          v          v
         Server A   Server B   Server C
```

Suppose we want requests for the same customer to reach the same backend instance.

We choose a routing key, such as:

```text
customerId
userId
tenantId
cacheKey
```

For example:

```text
Request 1:
customerId = 101

Request 2:
customerId = 101

Request 3:
customerId = 101
```

The load balancer hashes the customer ID and finds the first virtual node clockwise.

```text
hash("101") -> Server B
```

As long as the ring membership, virtual-node configuration, and hash function remain unchanged:

```text
Request 1 -> Server B
Request 2 -> Server B
Request 3 -> Server B
```

If Server B is removed, the requests map to the next available virtual node clockwise.

If a new server is added, only the keys whose successor changes are remapped.

### Why is this useful?

It can help with:

- Local caches on backend servers.
- Session affinity when sessions are stored locally.
- Partitioning requests by customer or tenant.
- Reducing cache misses when cluster membership changes.

### Does every request go to the same server?

Only if it uses the same routing key and the ring configuration is unchanged.

If the key is different, it may map to a different server.

For example:

```text
customerId = 101 -> Server B
customerId = 202 -> Server A
customerId = 303 -> Server C
```

Also, consistent hashing does not automatically balance CPU utilization or requests per second. A customer producing most of the traffic can still create a hot spot on one server.

---

## 14. What Is a Hot Key or Hot Spot?

Virtual nodes improve the distribution of hash-space intervals, but they cannot guarantee an equal distribution of actual workload.

Suppose a service uses customer ID as its routing key.

```text
Customer A -> 1,000 requests/minute
Customer B -> 100 requests/minute
Customer C -> 50 requests/minute
```

Suppose all three customer IDs map to Server B.

```text
Server B:
    Customer A -> 1,000 requests/minute
    Customer B -> 100 requests/minute
    Customer C -> 50 requests/minute
```

Server B now handles 1,150 requests per minute from these customers alone.

Even if the ring distributes the number of keys evenly, request volume can still be very uneven.

A **hot key** is a key that receives disproportionately high traffic or activity.

A **hot spot** is a resource or part of a system that receives excessive load relative to its capacity.

Possible solutions include:

- Caching frequently accessed data.
- Replicating read-heavy data across several servers.
- Splitting a hot partition where the data model permits it.
- Applying rate limits or admission control.
- Choosing a more appropriate routing key.
- Using weighted routing or other load-balancing mechanisms.
- Scaling the capacity of the affected service.

Do not randomly change the hash key for every request just to distribute load. That can destroy locality and create inconsistent routing for stateful data.

---

## 15. Weighted Virtual Nodes

Not all servers have the same capacity.

For example:

```text
Server A: 4 CPU cores
Server B: 8 CPU cores
Server C: 16 CPU cores
```

If every server receives exactly 100 virtual nodes, the stronger servers may be underutilized while the weaker servers receive too much work.

One approach is to allocate virtual nodes in proportion to server capacity.

For illustration:

```text
Server A -> 50 virtual nodes
Server B -> 100 virtual nodes
Server C -> 200 virtual nodes
```

Server C receives more ring positions because it has greater capacity.

This is called **weighted consistent hashing** when server weights influence the number of positions or the ownership distribution.

The exact weights should reflect meaningful capacity measurements. CPU cores alone may not represent actual throughput if servers have different memory, network, storage, or workload characteristics.

Weighted virtual nodes help distribute keys in proportion to capacity, but they do not guarantee that runtime traffic will be proportional to those weights.

---

## 16. A Complete End-to-End Example

Let's walk through the whole process.

### Initial cluster

```text
Physical servers:

Server A
Server B
Server C
```

Each server has 100 virtual nodes.

### Step 1: The client sends a request

```text
GET /customers/101
```

The load balancer extracts:

```text
Routing key = customerId 101
```

### Step 2: Hash the key

```text
keyHash = hash("101")
```

Assume the result is position 420.

### Step 3: Find the next virtual node

The ring searches clockwise for the first virtual-node position greater than or equal to 420.

Suppose the next virtual node is:

```text
Server B#17
```

### Step 4: Find the physical server

```text
Server B#17 -> Server B
```

The request is sent to Server B.

### Step 5: A new server is added

Server D joins the cluster with 100 virtual nodes.

Its positions are inserted into the ring.

If the next clockwise virtual node for key 101 changes from Server B#17 to Server D#42, future requests for customer 101 now go to Server D.

If the successor does not change, the request continues to go to its previous server.

### Step 6: A server is removed

Suppose Server D is removed.

Its virtual nodes are deleted from the ring.

The next surviving virtual node clockwise determines the new destination for affected keys.

The other mappings remain unchanged where their successors are unaffected.

---

## 17. Consistent Hashing vs. Rendezvous Hashing

Consistent hashing is not the only way to minimize remapping when the server set changes.

**Rendezvous hashing**, also called highest-random-weight hashing, calculates a score for each key-server pair and selects the server with the highest score.

Conceptually:

```text
score(key, Server A)
score(key, Server B)
score(key, Server C)

Choose the server with the highest score.
```

When a server is added or removed, rendezvous hashing also provides useful minimal-remapping behavior under the usual assumptions.

The main difference is the selection mechanism:

| Feature | Ring-based consistent hashing | Rendezvous hashing |
|---|---|---|
| Selection | First server clockwise | Highest-scoring server |
| Typical lookup | Sorted ring lookup | Score candidate servers |
| Virtual nodes | Commonly used | Not required in the basic algorithm |
| Useful for | Distributed caches, partition ownership, routing | Server selection, sharding, membership-based routing |

Rendezvous hashing may need to score every server for a lookup in a straightforward implementation, whereas a sorted ring can find the next position efficiently. Optimized implementations can change these practical trade-offs.

---

## 18. Consistent Hashing vs. Sticky Sessions

These concepts are related, but they are not identical.

| Feature | Sticky sessions | Consistent hashing |
|---|---|---|
| Main purpose | Keep a client attached to a server | Assign keys to servers with minimal remapping |
| Typical mechanism | Cookie or load-balancer session table | Hash key and ring lookup |
| Server membership changes | Depends on implementation; affinity may break | Only affected key intervals typically change |
| Works across requests | Yes, while affinity remains valid | Yes, for the same key and ring |
| Automatically replicates sessions | No | No |

Sticky sessions are often simpler if the goal is just to preserve a client-to-server association.

Consistent hashing is useful when we need a deterministic mapping from keys to a changing set of servers.

Neither approach removes the need to think about failover, shared session storage, replication, or server capacity.

---

## 19. Important Interview Questions

### Q1. What problem does consistent hashing solve?

It minimizes key remapping when servers are added or removed, unlike simple modulo hashing, which can remap a large proportion of keys when the number of servers changes.

### Q2. How does consistent hashing work?

It hashes servers and keys into the same circular hash space. A key is assigned to the first server position clockwise from its hash, wrapping around if necessary.

### Q3. Why do we need virtual nodes?

A single ring position per server can cause uneven ownership of the hash space. Virtual nodes give each physical server multiple positions, improving expected distribution and making reassignment occur across smaller intervals.

### Q4. What happens when a server is added?

The new server's positions are added to the ring. Keys whose clockwise successor changes move to the new server. Other keys typically retain their assignments.

### Q5. What happens when a server is removed?

Its positions are removed. Keys previously assigned to those positions move to their next surviving clockwise servers.

### Q6. Do virtual nodes mean that one physical server is duplicated?

No. They are logical ring positions. Each virtual node maps to its actual physical server.

### Q7. Does consistent hashing guarantee equal load?

No. It helps distribute hash-space ownership, but request frequencies, hot keys, unequal server capacity, and different workload costs can still create load imbalance.

### Q8. Does consistent hashing move the data automatically?

No. It determines the new mapping. Data migration, replication, cache refill, and recovery are separate responsibilities.

### Q9. Why is consistent hashing useful for distributed caching?

When a cache server is added or removed, only keys whose ring ownership changes typically need to move or be fetched again. This can reduce widespread cache misses and the resulting load on the underlying database.

### Q10. Is consistent hashing always better than round robin?

No. Round robin is often sufficient for stateless workloads where requests can be sent to any healthy backend. Consistent hashing is more useful when routing locality or stable key ownership matters.

---

## 20. Final Mental Model

Remember these five concepts:

1. **Load balancer:** Chooses a backend server for an incoming request.
2. **Hash ring:** Places keys and server positions in one circular hash space.
3. **Clockwise lookup:** Maps a key to the first server position clockwise.
4. **Minimal remapping:** Adding or removing a server changes only keys whose successor positions change.
5. **Virtual nodes:** Give each physical server multiple ring positions to improve distribution and make reassignment more granular.

The whole process can be summarized as:

```text
Incoming request
       |
       v
Extract routing key
       |
       v
Hash the key
       |
       v
Find the first virtual node clockwise
       |
       v
Identify its physical server
       |
       v
Forward the request
```

When the cluster changes:

```text
Add server:
    Insert its virtual nodes.

Remove server:
    Delete its virtual nodes.

For each key:
    Its new owner is determined by the first surviving
    virtual node clockwise from the key's hash.
```

**The central idea:** Consistent hashing does not prevent keys from moving when the cluster changes. It minimizes how many keys need to move, while virtual nodes improve the distribution of ownership across physical servers.
