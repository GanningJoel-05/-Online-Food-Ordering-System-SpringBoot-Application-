# Online Food Ordering System

A backend platform for a food delivery service (Swiggy/Zomato-style) — restaurants, menus, orders, and a fully enforced order lifecycle, built with production-grade patterns rather than plain CRUD.

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql&logoColor=white)
![JWT](https://img.shields.io/badge/Auth-JWT-black?logo=jsonwebtokens)
![Maven](https://img.shields.io/badge/Build-Maven-red?logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

---

## 📖 Overview

This is a REST API for a three-sided food ordering platform:

- **Customers** browse restaurants, place orders, and track them in real time
- **Restaurant owners** manage their menu and move incoming orders through preparation
- **Admins** have full oversight across the platform

The standout feature is an explicitly enforced **order status state machine** — every status change is validated against a transition map before it's allowed, so an order can never jump from `DELIVERED` back to `PLACED`, or skip straight from `PLACED` to `OUT_FOR_DELIVERY`.

---

## ✨ Features

**Authentication & Authorization**
- Stateless JWT authentication (no server-side sessions)
- Role-based access control — `CUSTOMER`, `RESTAURANT_OWNER`, `ADMIN`
- Two-layer authorization: role checks at the controller (`@PreAuthorize`) + resource-ownership checks in the service layer

**Restaurant & Menu Management**
- Full CRUD for restaurants, scoped to their owner
- Search and filter restaurants by cuisine/location with pagination and sorting
- Menu items with category, availability toggle, and optimistic-locked pricing

**Order Lifecycle**
- Order placement with server-side price calculation and per-item price snapshotting
- A formally defined order status state machine: `PLACED → CONFIRMED → PREPARING → OUT_FOR_DELIVERY → DELIVERED`, with a `CANCELLED` branch
- Illegal status transitions are rejected with a `409 Conflict`, not silently accepted
- Mock payment record created automatically on order placement

**Reliability & Data Integrity**
- Optimistic locking (`@Version`) on menu items — prevents lost updates when two requests modify the same item concurrently
- Scheduled background job auto-cancels orders left unconfirmed for too long
- Global exception handling — every error returns a consistent JSON shape, no leaked stack traces

**Developer Experience**
- Interactive API docs via Swagger UI, with a built-in "Authorize" button for testing JWT-protected routes directly in the browser
- Dockerized PostgreSQL for a one-command local setup
- Clean layered architecture: Controller → Service → Repository → Entity, with DTOs isolating the API contract from the database schema

---

## 🏗️ Architecture

```
Client (Postman / frontend)
        │
        ▼
JwtAuthFilter        →  validates JWT, populates Spring Security context
        │
        ▼
Controller           →  @Valid input validation, @PreAuthorize role checks
        │
        ▼
Service               →  business rules, ownership checks, @Transactional boundaries
        │
        ▼
Repository            →  Spring Data JPA
        │
        ▼
PostgreSQL
```

**Package structure**
```
com.foodapp
├── config/          Security config, JWT filter, OpenAPI config
├── security/         JWT generation & validation
├── entity/           JPA entities (User, Restaurant, MenuItem, Order, OrderItem, Payment)
├── repository/       Spring Data JPA interfaces
├── dto/              Request/response objects — entities never leave the service layer
├── service/          Business logic, including the order state machine
├── controller/       REST endpoints
└── exception/        Custom exceptions + global handler
```

---

## 🔄 The Order State Machine

This is the feature to lead with in interviews — a single source of truth for every legal transition, defined as a `Map<OrderStatus, Set<OrderStatus>>` in `OrderService`:

| From | Can move to |
|---|---|
| `PLACED` | `CONFIRMED`, `CANCELLED` |
| `CONFIRMED` | `PREPARING`, `CANCELLED` |
| `PREPARING` | `OUT_FOR_DELIVERY` |
| `OUT_FOR_DELIVERY` | `DELIVERED` |
| `DELIVERED` | *(terminal)* |
| `CANCELLED` | *(terminal)* |

Every transition request runs through one method, `transitionStatus()`, which checks this map and throws `InvalidOrderStateException` on anything not listed — turned into a clean `409 Conflict` by the global exception handler.

---

## 🧰 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security + JWT (`jjwt`) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| API Docs | springdoc-openapi (Swagger UI) |
| Build | Maven |
| Containerization | Docker Compose (for local Postgres) |

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven
- Docker (for PostgreSQL) — or a local Postgres instance

### Run it

```bash
# 1. Start PostgreSQL
docker-compose up -d

# 2. Run the app
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. Hibernate auto-creates all tables on first run (`ddl-auto: update`).

### Explore the API

Open **`http://localhost:8080/swagger-ui.html`** — every endpoint is listed with request/response schemas and a "Try it out" button. Click **Authorize**, paste a JWT from `/api/auth/login`, and test protected routes directly from the browser.

---

## 📮 API Reference

### Auth
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |

### Restaurants
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/restaurants?search=&page=&size=` | Public |
| GET | `/api/restaurants/{id}` | Public |
| POST | `/api/restaurants` | Owner / Admin |
| GET | `/api/restaurants/my-restaurants` | Owner / Admin |
| PUT | `/api/restaurants/{id}` | Owner / Admin |
| DELETE | `/api/restaurants/{id}` | Owner / Admin |

### Menu Items
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/restaurants/{restaurantId}/menu-items` | Public |
| POST | `/api/restaurants/{restaurantId}/menu-items` | Owner / Admin |
| PUT | `/api/restaurants/{restaurantId}/menu-items/{itemId}` | Owner / Admin |
| DELETE | `/api/restaurants/{restaurantId}/menu-items/{itemId}` | Owner / Admin |

### Orders
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/orders` | Customer |
| GET | `/api/orders/{id}` | Authenticated |
| GET | `/api/orders/my-orders` | Customer |
| GET | `/api/orders/restaurant/{restaurantId}` | Owner / Admin |
| PATCH | `/api/orders/{id}/status` | Owner / Admin |
| PATCH | `/api/orders/{id}/cancel` | Customer / Admin |

---

## 🧪 Testing

A full Postman walkthrough (registration → restaurant setup → order placement → state machine → authorization edge cases) is available in [`postman-workflow-guide.md`](./postman-workflow-guide.md).

---

## 🔮 Possible Extensions

- Flyway/Liquibase migrations instead of `ddl-auto: update`
- Real payment gateway integration (Razorpay/Stripe) with webhook handling
- Redis caching for restaurant search results
- WebSocket/SSE push notifications for live order status updates
- Testcontainers-based integration tests against a real PostgreSQL instance

---

## 👤 Author
**Ganning Joel J - Java Backend Developer**
Built as a placement portfolio project demonstrating layered Spring Boot architecture, JWT security, transactional integrity, and an explicit state-machine design for order lifecycle management.

*Feel free to fork, raise issues, or reach out with questions about the design decisions.*
