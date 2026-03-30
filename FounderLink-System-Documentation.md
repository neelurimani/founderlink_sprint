# FounderLink System Documentation (Updated)

## 1. Project Summary

FounderLink is a Spring Boot microservices platform for startup collaboration and funding workflows, with:
- JWT authentication and RBAC
- API Gateway routing (`/api/**`)
- Event-driven notifications/analytics via RabbitMQ
- Seeded local users for immediate testing
- Idempotent write APIs (header: `Idempotency-Key`)

## 2. Service Map and Ports

| Service | Port | Purpose |
|---|---:|---|
| `eureka` | 8761 | Eureka registry server |
| `config-global` | 8888 | Central config server |
| `api-gateway` | 8080 | Public API entrypoint |
| `user-service` | 8081 | Profiles/role-specific user data |
| `startup-service` | 8082 | Startup CRUD and approvals |
| `auth-service` | 8083 | Register/login/refresh tokens |
| `investment-service` | 8084 | Investment workflows |
| `messaging-service` | 8085 | REST + WebSocket messaging |
| `notification-service` | 8086 | Notification retrieval + email sender |
| `analytics-service` | 8087 | Funding analytics endpoints |

Outbound email in dev uses **log delivery** (`[FounderLink email]` in `notification-service` logs), not a fake SMTP container.

### Central configuration (repo)

Shared YAML for all services lives under **`global-config/`** at the repository root (Spring Cloud Config native backend). The `config-global` module copies these files into its JAR at build time; **`./scripts/docker-up-all.sh`** mounts `global-config/` read-only at `/config` in the config container so you can change settings and restart `founderlink-config` without a new image.

## 3. Seeded Data (Source of Truth)

### Auth service seed (`auth-service/src/main/resources/data.sql`)

| User ID | Name | Email | Role |
|---:|---|---|---|
| 1 | Default Admin | `admin@founderlink.local` | ADMIN |
| 2 | Seed Founder | `seed.founder@example.com` | FOUNDER |
| 3 | Seed Investor | `seed.investor@example.com` | INVESTOR |
| 4 | Seed CoFounder | `seed.cofounder@example.com` | COFOUNDER |

- **Seed password (plain text)**: `password`
- BCrypt hash in seed is the standard hash for `password`.

### User service seed (`user-service/src/main/resources/data.sql`)

| User ID | user_type | role column | Name | Email |
|---:|---|---|---|---|
| 1 | ADMIN | ROLE_ADMIN | Default Admin | `admin@founderlink.local` |
| 2 | FOUNDER | ROLE_FOUNDER | Seed Founder | `seed.founder@example.com` |
| 3 | INVESTOR | ROLE_INVESTOR | Seed Investor | `seed.investor@example.com` |
| 4 | COFOUNDER | ROLE_COFUNDER | Seed CoFounder | `seed.cofounder@example.com` |

Associated subtype rows:
- `founder`: id 2, startup_name `Seed Startup`, industry `SaaS`, funding_goal `100000.00`
- `investor`: id 3, investment_budget `500000.00`, preferred_industries `SaaS,FinTech`
- `co_founder`: id 4, expertise `Backend`, experience `5 years`

## 4. Authentication and RBAC

- Login: `POST /api/auth/login`
- Refresh: `POST /api/auth/refresh` (Bearer refresh token)
- Gateway validates JWT and forwards identity headers (`X-User-Id`, `X-Roles`) to downstream services.

Typical role usage:
- **FOUNDER**: create/update own startups
- **INVESTOR**: create/view own investments
- **COFOUNDER**: cofounder profile and messaging flows
- **ADMIN**: global reads, startup approvals, investment approvals, notifications-all

## 5. Idempotency Rules (Important)

FounderLink expects **`Idempotency-Key`** on mutating requests.

### Where to include it
- **Always include for**: `POST`, `PUT`, `DELETE`
- Recommended even for auth write-like requests (`login`, `refresh`) to avoid accidental duplicate replay behavior in shared workflows.

### Behavior
- **Same key + same request payload**: safe retry, usually returns same logical outcome.
- **Same key + different payload**: expected to fail (conflict/mismatch behavior).
- **New operation**: generate a **new** key.

### Client guidance
- Key format can be UUID, e.g. `a3f8d8dc-...`
- Keep it stable only while retrying the *same* operation.

## 6. API Checkpoint List (Gateway)

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`

### Users
- `POST /api/users`
- `POST /api/users/founders`
- `POST /api/users/cofounders`
- `POST /api/users/investors`
- `GET /api/users/{id}`
- `GET /api/users`
- `PUT /api/users/{id}`
- `PUT /api/users/founders/{id}`
- `PUT /api/users/cofounders/{id}`
- `PUT /api/users/investors/{id}`

### Startups
- `POST /api/startups`
- `GET /api/startups/{id}`
- `GET /api/startups`
- `PUT /api/startups/{id}`
- `DELETE /api/startups/{id}`

### Investments
- `POST /api/investments`
- `PUT /api/investments/{id}/approve`
- `GET /api/investments/investor/{id}`
- `GET /api/investments/startup/{id}`

### Messaging
- `POST /api/messages`
- `GET /api/messages/conversation/{id}`

### Notifications
- `GET /api/notifications`
- `GET /api/notifications/user/{userId}`

### Analytics
- `GET /api/analytics/funding-trends`
- `GET /api/analytics/reports`

## 7. Postman Collection and Environment Notes

- Import into Postman:
  - Collection: `postman/FounderLink.postman_collection.json`
  - Environment: `postman/FounderLink.postman_environment.json`
- Optional reference (seed emails/passwords by role): `postman/accounts-by-role.md` (not importable — see `postman/README.md`)
- Collection pre-request sets a fresh **GUID** into `idempotencyKey` before every request; mutating requests send `Idempotency-Key: {{idempotencyKey}}`.
- **Register (FOUNDER|INVESTOR|COFOUNDER|ADMIN)** use the same per-role env vars as login (`founderEmail`/`founderPassword`, etc.); defaults are non-seed `local.*@example.com` (see `postman/README.md`).
- **Login (…)** requests fill tokens for seed accounts.

## 8. Docker Runtime Notes (Current)

For stability in local Docker runs, ensure service containers receive required host mappings, especially:
- `SPRING_RABBITMQ_HOST=host.docker.internal`
- `SPRING_RABBITMQ_PORT=5672`

Without this, write flows that publish events can fail at runtime.

