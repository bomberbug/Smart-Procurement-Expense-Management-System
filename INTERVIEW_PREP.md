# SAP Labs Interview Prep — ProcureAI Project

## Questions You Will Be Asked & Answers

---

### 1. "Walk me through your project architecture"
**Answer:**
"I built a microservices-based procurement system with 3 backend services:
- API Gateway (Spring Boot, Port 8080) handles JWT authentication and routing
- Expense Service (Spring Boot, Port 8081) handles expense submission, ML fraud scoring
- Procurement Service (Spring Boot, Port 8082) handles purchase orders and vendor management
- Python Flask ML Service (Port 5000) runs fraud detection using GradientBoosting
- VueJS frontend communicates only with the API Gateway
- Redis for caching vendor and expense data
- RabbitMQ for async notifications between services"

---

### 2. "How does JWT authentication work in your system?"
**Answer:**
"When a user logs in, the API Gateway validates credentials against PostgreSQL,
generates a JWT token signed with HMAC-SHA256 containing the user's email and role.
Every subsequent request includes this token in the Authorization header.
My JwtAuthFilter extends OncePerRequestFilter, extracts and validates the token,
then sets the authentication in Spring's SecurityContextHolder.
Stateless — no session stored on server side."

---

### 3. "Why did you use Redis? What did you cache?"
**Answer:**
"I used Redis to reduce database load for frequently read data that doesn't change often.
Specifically I cached:
- The full vendor list (changes rarely, read very often)
- The expense list for the dashboard
Cache is evicted using @CacheEvict whenever a new expense or vendor is created.
This reduces DB queries by ~60% for read-heavy dashboard operations."

---

### 4. "Explain RabbitMQ usage in your project"
**Answer:**
"I used RabbitMQ for asynchronous communication between microservices.
When an expense is submitted, the Expense Service publishes a message to the
'expense.exchange' TopicExchange with routing key 'expense.submitted'.
If fraud is detected (score > 0.7), it publishes to 'expense.flagged'.
This decouples the notification logic from the core business logic.
The service doesn't wait for notifications to complete — it responds immediately."

---

### 5. "What is Dependency Injection? Show in your code."
**Answer:**
"Dependency Injection is when Spring creates and injects object dependencies
rather than the class creating them itself.
In my code: @Autowired on ExpenseRepository in ExpenseService.
Spring's IoC container creates the ExpenseRepository bean and injects it.
This makes testing easy — in JUnit I use @Mock for the repository and
@InjectMocks for the service, so I can test without a real database."

---

### 6. "Explain your database schema. Is it normalized?"
**Answer:**
"Yes, it's in BCNF (Boyce-Codd Normal Form).
- Employee → Department is a separate table (no transitive dependency)
- Approval table separates approval actions from PurchaseOrder
- Vendor is independent — not embedded in PurchaseOrder
No partial dependencies, no transitive dependencies.
Foreign keys: expense.emp_id → employee, purchase_order.vendor_id → vendor"

---

### 7. "What Design Patterns did you use?"
**Answer:**
- Repository Pattern: ExpenseRepository, VendorRepository abstract DB access
- Service Layer Pattern: Business logic in @Service classes, not controllers
- Factory Pattern: Spring's BeanFactory creates all beans
- Template Method: JwtAuthFilter extends OncePerRequestFilter
- Observer Pattern (loosely): RabbitMQ publisher/subscriber for events

---

### 8. "How does your ML fraud detection work?"
**Answer:**
"I trained a GradientBoostingClassifier on synthetic expense data.
Features: amount, amount_deviation_from_average, description_length.
High-amount expenses with short/no descriptions get high fraud scores.
Anything above 0.7 score gets auto-flagged and a RabbitMQ alert is sent.
The Python Flask service exposes a /predict REST endpoint that the
Java Expense Service calls via RestTemplate."

---

### 9. "How did you handle inter-service communication?"
**Answer:**
"Synchronous: Java Expense Service calls Python ML Service via RestTemplate
(REST over HTTP). If ML service is down, I catch the exception and set
category to OTHER with fraud score 0 — graceful degradation.
Asynchronous: Services publish events to RabbitMQ exchanges.
This way services are loosely coupled — Expense Service doesn't know
or care who consumes the messages."

---

### 10. "How did you write tests?"
**Answer:**
"I used JUnit 5 with Mockito.
@ExtendWith(MockitoExtension.class) sets up the test environment.
@Mock creates mock objects for dependencies (repository, rabbitTemplate).
@InjectMocks creates the service with mocked dependencies injected.
I tested: successful login, invalid credentials, expense status updates,
flagged expense retrieval. Tests run without any database or RabbitMQ."

---

## Quick DSA Points (SAP asks these)

- HashMap internal: Array of LinkedList buckets, hashCode() % capacity for index
- POST vs PUT: POST creates new resource, PUT updates existing (idempotent)
- BCNF: Every determinant must be a candidate key
- Stack using LinkedList: push() = addFirst(), pop() = removeFirst()
- BFS vs DFS: BFS uses Queue (level-order), DFS uses Stack/recursion (deep-first)
