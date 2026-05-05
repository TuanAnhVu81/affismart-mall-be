# AffiSmart Mall — Backend

> **A full-featured e-commerce backend built for portfolio and learning.**
> Designed to demonstrate real-world Java engineering skills for a Fresher Software Engineer role.

[![Java](https://img.shields.io/badge/Java-21-007396?logo=java&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![FastAPI](https://img.shields.io/badge/FastAPI-Python-009688?logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/)
[![License](https://img.shields.io/badge/License-Portfolio-lightgrey)](./LICENSE)

---

## 🔗 Live Demo & Access

- **Storefront:** [avt-affismart-mall.vercel.app](https://avt-affismart-mall.vercel.app)
- **Backend API:** [affismart-mall-be.onrender.com](https://affismart-mall-be.onrender.com/swagger-ui.html) (Swagger UI)

### Demo Accounts

| Role | Email | Password |
|---|---|---|
| **Admin** | `admin@gmail.com` | `12345678A` |
| **Customer** | `binhvt@gmail.com` | `12345678b` |
| **Affiliate** | `anhvt@gmail.com` | `12345678a` |

> [!NOTE]
> **Cold Start:** Since this project is hosted on Render's free tier, the services may go to sleep after 15 minutes of inactivity. I have configured a monitor to keep it awake during business hours (**8:00 AM – 5:00 PM**). If you access it outside this window, please allow up to **5 minutes** for the servers to wake up.

---

## About This Project

**AffiSmart Mall** is a backend-focused e-commerce platform built to simulate production-grade requirements for a fresher portfolio. It covers the full lifecycle of a shopping platform — from customer registration and product browsing, to Stripe payment processing, affiliate marketing, commission tracking, admin management, and AI-powered product recommendations with a shopping chatbot.

The project is structured as a **Modular Monolith** with a separated **FastAPI AI microservice**, demonstrating clean module boundaries, object-oriented design, layered architecture, and integration with third-party services.

> **AI-assisted development:** This project was built with the support of AI coding assistants (Gemini, Claude, Codex) for code generation, unit testing, and documentation — with full understanding, review, and ownership of all code by the developer.

---

## ✨ Key Highlights

| Capability | Details |
|---|---|
| 🔐 **Authentication** | JWT access tokens + rotating refresh tokens via Redis, multi-device session control |
| 🛒 **E-commerce Core** | Product catalog, order flow, Stripe Checkout, webhook-based fulfillment |
| 🤝 **Affiliate System** | Referral link generation, click fraud protection, commission snapshots, payout workflow |
| 📊 **Analytics** | Admin dashboard with order, revenue, affiliate, and product performance metrics |
| 🤖 **AI Features** | Collaborative filtering recommendations + Gemini-powered shopping chatbot with guardrails |
| 🧪 **Testing** | JUnit 5 + Mockito unit tests across auth, product, order, payment, affiliate, AI, and analytics |
| 🚀 **Deployment** | Dockerized, deployed on Render (Java + Python), frontend on Vercel |


## Tech Stack

| Area | Technology |
|---|---|
| Core Backend | Java 21, Spring Boot 3.5 |
| Security | Spring Security, JWT, Redis refresh sessions |
| Database | PostgreSQL, Spring Data JPA, Hibernate |
| Migration | Flyway |
| Cache and Rate Limit | Redis, Caffeine |
| Payment | Stripe Checkout, Stripe Webhook |
| File Storage | Cloudinary |
| API Docs | Springdoc OpenAPI, Swagger UI |
| AI Service | Python, FastAPI, SQLAlchemy, scikit-learn, Google GenAI |
| Testing | JUnit 5, Mockito, Spring Security Test |
| Build | Maven Wrapper |
| Deployment Ready | Dockerfile for Java backend and AI service |

## Architecture

The project follows a Modular Monolith architecture for the Java backend. Each business area has its own controller, service, repository, entity, DTO, and mapper layer where needed.

The AI logic is separated into a small FastAPI service. The Java backend calls this service through internal HTTP APIs. This keeps the main business system stable while allowing AI features to grow independently.

```text
Frontend
   |
   | REST API
   v
Spring Boot Backend
   |-- Auth and User
   |-- Product and Category
   |-- Order and Payment
   |-- Affiliate and Payout
   |-- Analytics
   |-- AI Proxy
   |
   | SQL / Redis / External APIs
   v
PostgreSQL, Redis, Stripe, Cloudinary
   |
   | Internal REST call
   v
FastAPI AI Service
   |-- Recommendation model
   |-- Chatbot guardrails
   v
Google GenAI
```

## Main Features

### Customer

- Register, login, refresh token, logout.
- View and search active products.
- View product and category details.
- Create orders from cart items.
- Create Stripe Checkout sessions.
- View order history and order detail.
- Cancel allowed orders.
- Use AI recommendations and AI shopping assistant.

### Affiliate

- Register as an affiliate partner.
- Create referral links for homepage or product-specific traffic.
- Track referral clicks with Redis-based fraud protection.
- View dashboard metrics, referral links, commissions, and payouts.
- Request payout when balance is eligible.

### Admin

- Manage users and user status.
- Manage products and categories, including inactive items.
- Upload product images through Cloudinary.
- Review and update orders.
- Approve affiliate accounts and set commission rates.
- Review payout requests.
- View analytics dashboard and top performance data.
- Manage blocked IPs from affiliate fraud tracking.

### AI

- Log recommendation events from user behavior.
- Provide homepage recommendations and related product recommendations.
- Proxy chatbot requests to the FastAPI AI service.
- Rate limit AI chat requests with Redis to protect API usage.
- Use simple guardrails to keep chatbot answers inside shopping-related topics.

## Project Structure

```text
src/main/java/com/affismart/mall
|-- common          # Shared response, error code, base entity, enums
|-- config          # Security, CORS, cache, Swagger, async, external configs
|-- exception       # AppException and GlobalExceptionHandler
|-- integration     # Stripe and Cloudinary adapters
|-- modules
|   |-- auth        # Login, JWT, refresh tokens
|   |-- user        # Profile and admin user management
|   |-- product     # Product and category catalog
|   |-- order       # Order creation, order status, commission trigger
|   |-- payment     # Stripe Checkout and webhook
|   |-- affiliate   # Affiliate account, links, clicks, payouts
|   |-- analytics   # Admin dashboard metrics
|   |-- ai          # Event logging, AI proxy, chat rate limit
|   |-- health      # Health endpoints
|
src/main/resources
|-- application.yaml
|-- application-dev.yaml
|-- application-prod.yaml
|-- db/migration    # Flyway SQL migrations

affismart-ai-service
|-- app
|   |-- api         # FastAPI routes
|   |-- schemas     # Pydantic request and response models
|   |-- services    # Recommendation and chat logic
|-- requirements.txt
|-- Dockerfile
```

## Getting Started

### Requirements

- Java 21
- Maven Wrapper, already included in this repo
- PostgreSQL
- Redis
- Python 3.11, only needed for the AI service
- Stripe CLI, optional for local webhook testing

### 1. Configure Environment Variables

Create a local `.env` file from the example file:

```powershell
Copy-Item .env.example .env
```

At minimum, update these values:

```env
JWT_SECRET=replace-with-a-strong-random-secret-at-least-32-bytes-long
ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=replace-with-a-strong-admin-password
```

If you want to test optional integrations, also set:

```env
STRIPE_ENABLED=true
STRIPE_SECRET_KEY=...
STRIPE_WEBHOOK_SECRET=...

CLOUDINARY_ENABLED=true
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...

GEMINI_API_KEY=...
GEMINI_MODEL=gemini-3-flash-preview
```

### 2. Prepare PostgreSQL and Redis

For local development, the default profile expects:

```text
PostgreSQL: localhost:5432
Database:   affismart_mall
Username:   postgres
Password:   postgres

Redis:      localhost:6379
```

Create the database before starting the backend:

```sql
CREATE DATABASE affismart_mall;
```

Flyway will create and update tables automatically when the app starts.

### 3. Run the Spring Boot Backend

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux:

```bash
./mvnw spring-boot:run
```

The backend will start at:

```text
http://localhost:8080
```

### 4. Run the AI Service

The AI service is optional for basic e-commerce flows, but required for recommendations and chat.

```powershell
cd affismart-ai-service
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

The AI service will start at:

```text
http://localhost:8000
```

## API Documentation

Swagger UI is available after the backend starts:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

FastAPI docs:

```text
http://localhost:8000/docs
```

## Useful Local URLs

| Service | URL |
|---|---|
| Spring Boot API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| FastAPI AI Service | `http://localhost:8000` |
| FastAPI Docs | `http://localhost:8000/docs` |
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |

## Testing

Run all Java tests:

```powershell
.\mvnw.cmd test
```

Run tests without copying main resources:

```powershell
.\mvnw.cmd "-Dmaven.resources.skip=true" test
```

The test suite covers important service logic such as authentication, JWT, refresh tokens, products, orders, payment webhook, affiliate tracking, payout handling, AI proxy, AI rate limiting, analytics, and caching.

## Docker

Build the Java backend image:

```powershell
docker build -t affismart-mall-be .
```

Build the AI service image:

```powershell
docker build -t affismart-ai-service .\affismart-ai-service
```

Run containers with the needed environment variables for database, Redis, Stripe, Cloudinary, and AI service URLs.

## Security Notes

- Access tokens are JWT-based.
- Refresh tokens are stored and rotated with Redis.
- Admin, customer, and affiliate APIs are protected by Spring Security method rules.
- Input DTOs use Jakarta Bean Validation.
- Global error handling returns clean API errors and does not expose stack traces in responses.
- AI chat calls are rate limited with Redis to protect the AI API key.
- Stripe webhooks are verified with webhook signatures.

## Performance Notes

- `open-in-view` is disabled to avoid hidden lazy loading issues.
- Flyway indexes support hot query paths for orders, commissions, payouts, referral links, order items, and recommendation events.
- Product search uses PostgreSQL trigram indexes.
- Category data and analytics data use cache where suitable.
- AI client calls have connect and read timeouts to avoid blocking backend threads for too long.

## Why This Project Fits a Fresher Java Role

This project is designed to reflect real backend engineering work and maps directly to common fresher Java requirements:

| JD Requirement | How This Project Addresses It |
|---|---|
| **Java & Spring Boot web development** | Full Spring Boot 3.5 REST API with layered architecture |
| **OOP & design principles** | Service/Repository/Controller separation, dependency injection, custom exceptions, mappers |
| **Database & SQL** | PostgreSQL with JPA/Hibernate, Flyway migrations, relational schema design |
| **Unit testing & Definition of Done** | JUnit 5 + Mockito tests across all core modules before code review |
| **API documentation** | Swagger/OpenAPI with Springdoc, self-documenting endpoints |
| **Third-party integration** | Stripe (payment), Cloudinary (storage), Redis (caching/session), Google GenAI (AI) |
| **Agile/Scrum readiness** | Feature-based modular structure, clear commit history, deployable increments |
| **AI tool experience** | Built with Gemini and Claude for code generation, test writing, and documentation |


## Future Improvements

- Add GitHub Actions for build and test automation.
- Add Docker Compose for local PostgreSQL, Redis, backend, and AI service.
- Add more integration tests for controllers and Stripe webhook flow.
- Add monitoring, request tracing, and structured logs.
- Add real shipping provider integration.
- Add coupon, review, and rating features.

## License

This project is built for learning and portfolio use.
