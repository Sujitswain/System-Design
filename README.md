# Simple System Design Roadmap (HLD + LLD)

A beginner-friendly roadmap for a Java backend developer. Follow it in order. **Do not try to learn everything at once.**

**Time:** 60–90 minutes a day
**Plan:** 6 weeks, with HLD and LLD studied together
**Main tools:** Hello Interview to learn, roadmap.sh to check topics, Eraser to draw diagrams.

---

## First, understand the difference

- **HLD (High-Level Design):** How the whole system works — services, databases, cache, queues and how they communicate.
- **LLD (Low-Level Design):** How the code is organised — classes, interfaces, methods and how objects work together.
- **Machine coding:** Actually writing a working Java solution for a design problem.

Example: For a parking app, HLD is how the app, backend and database communicate. LLD is the Java design for `ParkingLot`, `ParkingSpot`, `Vehicle` and `Ticket`.

---

## Step 1 — Learn the basics

Do these first. If you already know a topic, revise it quickly and move on.

### HLD basics
- [ ] What happens when you open a website?
- [ ] Client, server and API
- [ ] HTTP and REST APIs
- [ ] What is a database? SQL vs NoSQL
- [ ] What is a database index?
- [ ] What is caching? Why use Redis?
- [ ] What is a load balancer?
- [ ] What is a message queue? Why use Kafka?
- [ ] What do scaling and availability mean?
- [ ] What are timeouts, retries and failures?

### LLD basics
- [ ] Classes and objects
- [ ] Encapsulation, inheritance and polymorphism
- [ ] Interface vs abstract class
- [ ] Composition vs inheritance
- [ ] SOLID principles
- [ ] UML class diagram basics
- [ ] Unit testing basics

**Move on when:** You can explain each item simply and give one example.

---

## Step 2 — Learn a few important design patterns

Do not try to memorise every pattern. Start with these five.

- [ ] **Strategy:** Choose between different behaviours, such as different pricing rules.
- [ ] **Factory:** Create the right object without spreading creation logic everywhere.
- [ ] **Observer:** Notify other components when something happens.
- [ ] **State:** Change behaviour based on the current state, such as a vending machine.
- [ ] **Builder:** Create an object with many optional values clearly.

For each pattern:
1. Understand the problem it solves.
2. Look at a small Java example.
3. Write your own example.
4. Ask when you would *not* use it.

---

## Step 3 — Practise HLD designs

Study one design at a time. First understand a solved example, then close it and draw your own.

### Start with these three
- [ ] **URL shortener** — like a service that turns a long URL into a short link.
- [ ] **Rate limiter** — restrict how many requests a user can make.
- [ ] **Notification system** — send email, SMS or push notifications.

### Then practise these
- [ ] **File storage** — upload and download files.
- [ ] **Movie ticket booking** — choose seats and prevent two users from booking the same seat.
- [ ] **Payment system** — process payments safely and handle retries.
- [ ] **Chat app** — send and receive messages.
- [ ] **News feed** — show posts from people you follow.
- [ ] **Video streaming** — upload and watch videos.

### For every HLD question, follow the same steps

1. **Ask what is needed:** What should the system do?
2. **List the main parts:** App, backend service, database, cache or queue if needed.
3. **Draw the flow:** Show how a request travels through the system.
4. **Explain the database:** What data do we store and why?
5. **Think about problems:** What if the database or another service is down? What if a request is repeated?
6. **Explain your choices:** Why use a cache or queue? What alternatives exist?

You do not need a perfect design on the first attempt.

---

## Step 4 — Practise LLD / Java design problems

For each problem, first draw the classes, then implement the main features in Java.

### Start with these
- [ ] **Parking lot** — vehicles, parking spots and tickets.
- [ ] **Vending machine** — products, payment and dispensing.
- [ ] **Tic-tac-toe** — board, players and game rules.
- [ ] **LRU cache** — store recent items and remove the least recently used item when full.

### Then practise these
- [ ] **Movie ticket booking** — shows, seats, bookings and seat availability.
- [ ] **Splitwise** — expenses, splits and balances.
- [ ] **Elevator system** — elevators, floors and requests.
- [ ] **Notification system** — different notification channels.
- [ ] **Library system** — books, members and borrowing.

### For every LLD question, follow the same steps

1. Clarify what the program must do.
2. Identify the main classes.
3. Decide what each class is responsible for.
4. Connect classes using fields and interfaces.
5. Write the important methods.
6. Implement the core use cases.
7. Test normal cases and edge cases.
8. Try adding one new requirement without rewriting everything.

Do not force design patterns into every problem. Keep the code understandable.

---

## Step 5 — Follow this 6-week plan

| Week | Learn | Practise |
|---|---|---|
| **1** | HLD basics: APIs, databases, cache, load balancer. LLD basics: classes, interfaces, SOLID. | Draw a URL shortener; model a parking lot. |
| **2** | Redis, Kafka, retries and failures. Learn Strategy and Factory. | Design a rate limiter; implement a vending machine. |
| **3** | Learn the HLD interview approach. Learn Observer and State. | Design notifications; implement tic-tac-toe or LRU cache. |
| **4** | Database consistency, concurrency and safe retries. | Design movie ticket booking; implement its core classes. |
| **5** | File storage, chat and news feeds. | Design file storage; implement Splitwise or elevator. |
| **6** | Revise weak topics and practise explaining trade-offs. | Complete two timed HLD designs and two timed Java LLD problems. |

If a week takes longer, that is fine. Understanding matters more than finishing on a fixed date.

---

## Step 6 — Simple daily routine

For a 60-minute session:

- [ ] **20 min:** Learn one concept from Hello Interview or another trusted resource.
- [ ] **20 min:** Apply it to a design or write a small Java example.
- [ ] **15 min:** Draw the design in Eraser or on paper.
- [ ] **5 min:** Write down what you learned and what is still confusing.

On days when you have more time, spend it implementing a complete problem or explaining a design out loud.

---

## How to use your resources

### 1. Hello Interview — learn the topic
Read/watch one topic at a time. Do not jump between many case studies. After learning a design, explain it in your own words.

- [System Design learning path](https://www.hellointerview.com/learn/system-design/overview)
- [System Design in a Hurry](https://www.hellointerview.com/learn/system-design/in-a-hurry/introduction)

### 2. roadmap.sh — check what you might be missing
Use it as a checklist. You do not need to master every advanced item immediately.

- [System Design roadmap](https://roadmap.sh/system-design)

### 3. Eraser — draw what you understand
Use it to make:
- [ ] HLD architecture diagrams
- [ ] LLD class diagrams
- [ ] Sequence diagrams showing the order of calls

**Good practice:** Draw your own first, compare it with an AI-generated diagram, fix mistakes, and then redraw it without help.

Example prompt for Eraser:

> Draw a simple URL shortener system. Show the user, API/backend service, database and cache. Use arrows to show what happens when a user creates a short URL and when someone opens it. Keep the diagram simple and explainable in an interview.

Example LLD prompt:

> Draw a simple Java parking lot class diagram with ParkingLot, ParkingSpot, Vehicle and Ticket. Show how the classes relate to each other. Keep it beginner-friendly and avoid unnecessary design patterns.

---

## When are you ready to move on?

- [ ] I can explain the basic HLD terms in my own words.
- [ ] I can draw a simple system and explain the request flow.
- [ ] I can explain why the system needs a database, cache or queue.
- [ ] I can identify what might fail and what the system should do.
- [ ] I can design Java classes for a small problem.
- [ ] I can implement at least one LLD problem without copying a solution.
- [ ] I can explain and change my design when requirements change.

You do **not** need to tick every box before continuing. Learn, practise, and revisit weak topics.

---

## Your first 7 days

- [ ] **Day 1:** Understand HLD vs LLD. Learn client, server and API.
- [ ] **Day 2:** Revise HTTP, REST APIs and databases.
- [ ] **Day 3:** Learn indexes, caching and Redis.
- [ ] **Day 4:** Learn load balancers, queues and the basic purpose of Kafka.
- [ ] **Day 5:** Learn Java interfaces, SOLID and Strategy.
- [ ] **Day 6:** Draw a URL shortener in Eraser.
- [ ] **Day 7:** Draw a parking lot class diagram and write a small Java implementation.

## Keep this rule in mind

**Learn one thing → try it yourself → draw or code it → explain it simply → revise it later.**

Do not aim to memorise every system design. Aim to understand why each decision is made.
