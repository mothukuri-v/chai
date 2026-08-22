# ☕ ChaiPass — Tea Subscription Platform

"One QR. One tea. Every day."

A production-grade, 5-tier subscription platform that lets a customer pay once a month
and redeem **one tea per day** at any participating tea shop by scanning a QR code.

## Architecture

```
┌─────────────────────┐
│   React.js SPA       │  Customer / Shop Owner / Admin dashboards (responsive)
└──────────┬───────────┘
           │ HTTPS (JWT)
┌──────────▼───────────┐
│ Node.js + Express     │  API Gateway / BFF
│ - Auth issuance       │  - Request shaping, rate limiting, input validation
│ - File/QR image I/O   │  - Razorpay webhook ingestion
│ - Aggregation for UI  │  - Proxies business rules to Spring Boot
└──────────┬───────────┘
           │ Internal REST (service token)
┌──────────▼───────────┐
│ Java + Spring Boot     │  Business / Enterprise Services
│ - Subscription engine  │  - QR validation engine
│ - Redemption rules     │  - Fraud / duplicate prevention
│ - Payment reconciliation
└──────────┬───────────┘
           │ Spring Data
┌──────────▼───────────┐
│ Data Access Layer      │  Repositories, transactions, indices
└──────────┬───────────┘
┌──────────▼───────────┐
│ MongoDB                │  users, shops, subscriptions, redemptions, payments, audit_logs
└───────────────────────┘
```

**Why split Node and Java?** Node/Express is a thin, fast **API Gateway (BFF)** — auth issuance,
request shaping, QR image generation, Razorpay webhook ingestion, and response aggregation for
the three dashboards. Spring Boot owns **all business rules that must never be wrong**:
subscription validity, one-tea-per-day enforcement, QR authenticity, and payment reconciliation.
This isolates the money- and trust-critical logic behind strong typing, Bean Validation, and
transactional MongoDB writes, independent of the gateway's release cadence.

## Repository layout

```
tea-subscription/
├── frontend/            React 18 SPA (Vite), 3 role-based dashboards
├── gateway/              Node.js + Express API Gateway / BFF
├── business-service/     Java 21 + Spring Boot 3 business services
├── infra/                Docker Compose, Jenkinsfile, Nginx config, AWS notes
└── docs/                 Architecture, API contract, data model, runbook
```

## Quick start (local, Docker)

```bash
cd infra
cp .env.example .env        # fill in JWT_SECRET, RAZORPAY_KEY_ID/SECRET, MONGO_URI
docker compose up --build
```

| Service          | URL                          |
|-------------------|-------------------------------|
| Frontend          | http://localhost:5173         |
| Gateway (Node)     | http://localhost:4000/api     |
| Business (Spring)  | http://localhost:8080/internal (not public — gateway-only) |
| MongoDB            | mongodb://localhost:27017     |

See `docs/ARCHITECTURE.md`, `docs/API.md`, and `docs/DATA_MODEL.md` for full details, and
`docs/SECURITY_RULES.md` for how duplicate redemptions, fake QR codes, expired subscriptions,
and duplicate payments are specifically prevented.

## Going live on a real domain

`docker-compose.yml` above is for local development only. To put the stack behind a real
domain with automatic HTTPS (Caddy reverse proxy, no ports exposed except 80/443), see
**`docs/DEPLOY.md`** — it walks through DNS, EC2 security group rules, and the
production compose overlay (`infra/docker-compose.prod.yml`) step by step.
