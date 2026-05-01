# Smart Procurement & Expense Management System

A microservices-based enterprise procurement system built with Java Spring Boot, Python Flask ML service, and VueJS frontend.

## Architecture

```
VueJS Frontend (Port 5173)
        │
API Gateway - Spring Boot (Port 8080)  ← JWT Auth
        │
   ┌────┴────┐
   │         │
Expense   Procurement
Service    Service
(8081)     (8082)
   │         │
   └────┬────┘
        │
   PostgreSQL (5432)
        │
   Python ML Service (5000)  ← Fraud Detection
        │
   Redis Cache (6379)
        │
   RabbitMQ (5672)
```

## Tech Stack

| Layer | Technology |
|-------|------------|
| API Gateway | Java 17 + Spring Boot 3 + JWT |
| Microservices | Spring Boot x2 |
| ORM | Hibernate + PostgreSQL |
| Cache | Redis |
| Message Queue | RabbitMQ |
| ML Service | Python Flask + scikit-learn |
| Frontend | VueJS 3 + Bootstrap 5 |
| Auth | Spring Security + JWT |
| Testing | JUnit 5 + Mockito |
| Container | Docker + Docker Compose |
| Build | Maven |

## Quick Start

### Prerequisites
- Java 17+
- Python 3.9+
- Node.js 18+
- Docker & Docker Compose

### Run with Docker (Recommended)
```bash
docker-compose up --build
```

### Run Manually

**1. Start Infrastructure**
```bash
docker-compose up postgres redis rabbitmq -d
```

**2. Start ML Service**
```bash
cd ml-service
pip install -r requirements.txt
python app.py
```

**3. Start API Gateway**
```bash
cd backend-gateway
mvn spring-boot:run
```

**4. Start Expense Service**
```bash
cd expense-service
mvn spring-boot:run
```

**5. Start Procurement Service**
```bash
cd procurement-service
mvn spring-boot:run
```

**6. Start Frontend**
```bash
cd frontend
npm install
npm run dev
```

## Access URLs
- Frontend: http://localhost:5173
- API Gateway: http://localhost:8080
- Expense Service: http://localhost:8081
- Procurement Service: http://localhost:8082
- ML Service: http://localhost:5000
- RabbitMQ Console: http://localhost:15672 (guest/guest)

## Default Login Credentials
| Role | Username | Password |
|------|----------|----------|
| Admin | admin@company.com | admin123 |
| Manager | manager@company.com | manager123 |
| Employee | employee@company.com | employee123 |
| Finance | finance@company.com | finance123 |

## Database Schema (BCNF Normalized)

```sql
Employee (emp_id, name, dept_id, role, email)
Department (dept_id, name, budget, manager_id)
Expense (exp_id, emp_id, amount, category, fraud_score, status, created_at)
PurchaseOrder (po_id, vendor_id, dept_id, amount, status, approved_by)
Vendor (vendor_id, name, rating, contact, email)
Approval (approval_id, po_id, approver_id, action, timestamp)
```

## Features
- Role-based access (Admin, Manager, Employee, Finance)
- Expense submission with ML fraud detection
- Auto expense classification (Travel/Food/Equipment/Other)
- Purchase order workflow with multi-level approvals
- Redis caching for vendor and budget data
- Async email notifications via RabbitMQ
- Analytics dashboard with charts
- Docker containerized deployment
- JUnit + Mockito unit tests
