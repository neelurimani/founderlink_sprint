# FounderLink Architecture Audit Report

**Date:** 2026-03-29  
**Reference:** FounderLink Complete Case Study (system design document)  
**Scope:** Backend microservices in this repository.

---

## 1. Misalignments Found

| Area | Design expectation | Previous state |
|------|-------------------|----------------|
| Auth register | No JWT on registration; login issues tokens | `POST /auth/register` returned `accessToken` + `refreshToken` |
| Team service | §7.5 APIs: invite, join, list by startup | **Missing module** in Maven reactor |
| User HTTP layer | Controllers as thin adapters | Package named `api` instead of `controller` |
| Investment integrity | Unique (startup, investor); amount &gt; 0 | No DB-level unique constraint or check |
| Config | Centralized external configuration | Per-service `application.properties` duplicated datasource, JPA, etc. |
| Error contract | Consistent JSON error envelope | Auth used ad-hoc `ErrorResponse` record without `timestamp` / `path` |
| Validation | Strict email/password/name rules at DTO layer | Password length only on register; email relied on loose `@Email` |

**Doc vs implementation (intentional extensions — not removed):**

- **Analytics service** appears in HLD but not in §7 API tables; retained as an event/analytics consumer.
- **User service** extended profiles (`/users/founders`, `/users/investors`, etc.) beyond the minimal §7.2 table; they implement §4.2 profile fields for each persona.
- **Investment `PUT .../approve`** supports §4.5 status workflow (PENDING/APPROVED/…) though not listed in the short API summary table.
- **Startup search/filters** (§4.4) may still be partial vs “industry, stage, goal, location”; report as **remaining gap** if endpoints do not expose all filters.

---

## 2. Fixes Applied

### Authentication (auth-service)

- **`POST /auth/register`** returns **201 Created** with body  
  `{ "message", "userId", "email" }` — **no JWT fields**.
- **JWT pair only from** `POST /auth/login` and refresh flow unchanged logically (access from `POST /auth/refresh`).
- **DTO validation:** regex for email, password (upper/lower/digit/special, 8–128), person name (letters and spaces).
- **Config split:** `JwtConfig`, `JwtProperties`, `ModelMapperConfig`, `PasswordEncoderConfig` (replaces duplicate `SecurityBeans` BCrypt beans). `SecurityConfig` remains dedicated to HTTP security.
- **`JwtService`:** uses `JwtProperties` for TTL; secret from `security.jwt.secret` (Config Server).
- **`ApiErrorResponse` + `GlobalExceptionHandler`:** standardized fields (`timestamp`, `status`, `error`, `message`, `path`, optional `details`). Handles `DataIntegrityViolationException` as **409**.
- **Bootstrap:** `application.properties` reduced to Config Server import + `fail-fast=true`; full runtime config in **`global-config/auth-service.yml`** (in-repo source for the Config Server artifact).

### Team service (new module: `team-service`)

- Port **8088** (`TEAM_SERVICE_PORT`).
- **Entity `TeamMembership`** with **unique (`startup_id`, `user_id`)** and statuses PENDING → ACTIVE on join.
- **APIs:** `POST /teams/invite` (FOUNDER), `POST /teams/join` (authenticated invitee), `GET /teams/startup/{startupId}`.
- **Gateway:** `/api/teams` → team-service (see `global-config/api-gateway.yml`).
- **DB:** `team_service` schema; **nuke script** updated.

### User service

- **`UserProfileController`** moved from package `api` → **`controller`** (clean architecture naming).

### Investment service

- **JPA:** `UNIQUE (startup_id, investor_id)`, **`@Check(amount > 0)`**, `NOT NULL` on key columns.
- **Service:** catch `DataIntegrityViolationException` on create → **409** with clear message.

### Tooling / repo

- **`scripts/mysql-nuke-founderlink.sh`:** drops `team_service` database.
- **`PORTS.md`:** documents team-service **8088**.
- **Postman:** register flows expect **201** and persist `userId` into role-specific env vars (**no tokens** from register).

---

## 3. Validation Improvements

- Auth: centralized `ValidationPatterns` + Bean Validation on `RegisterRequest` / `LoginRequest`.
- Investment: `@Positive` already on amount; DB **CHECK** reinforces.
- Team: `@NotNull` / `@Positive` / `@NotBlank` on invite/join DTOs.

**Still recommended (not fully rolled out everywhere):** apply the same email/name/password regex classes to **user-service** profile DTOs and **startup** DTOs for full parity.

---

## 4. Config Refactoring

- **auth-service** and **team-service** local files: **bootstrap + mandatory Config Server** only.
- **Shared behavior** remains in **`global-config/*.yml`** (packaged into `config-global` at build).

**Note:** “GitHub Config Server” in your brief is interpreted as **hosted central config** (this repo uses the **`config-global`** Spring Cloud Config Server, not file-based fallbacks in each service). Replacing the Git remote for Config Server is an **ops** step outside this codebase.

---

## 5. Database Changes

- **auth:** existing `users.email` unique retained.
- **investment:** unique constraint name `uk_investment_startup_investor`; Hibernate `@Check` for `amount > 0`.
- **team:** new schema `team_service`, table `team_members` with unique `uk_team_startup_user`.

**Nuke / recreate:** run `./scripts/mysql-nuke-founderlink.sh` then restart services so Hibernate (or future Flyway) rebuilds schema with new constraints.

---

## 6. Remaining Risks / Follow-up

1. **Standard error envelope** is implemented in **auth-service** only; other services should adopt the same `ApiErrorResponse` + `@ControllerAdvice` shape.
2. **`@Configuration` explosion:** user-service, startup-service, messaging-service, etc. still consolidate Rabbit, security, CORS in fewer/smaller classes in some places — incremental split recommended.
3. **Founder verifies startup ownership on team invite:** current rule is **role FOUNDER** only; a production hardening step is to **validate `startupId` belongs to the inviting founder** via startup-service (Feign).
4. **Team Rabbit event `TEAM_INVITE_SENT`:** not yet published; align with §8 when notifications are wired.
5. **Startup discovery filters** (§4.4): confirm query params cover industry, stage, funding goal, location.
6. **strict config import** breaks local runs if Config Server is down — expected for production-like dev; tests rely on **`src/test/resources/application.properties`** overrides (auth-service pattern).

---

## 7. Verification Performed

- `mvn test` on **auth-service** (register → no tokens; login → tokens).
- `mvn test` on **user-service** after controller move.
- `mvn compile` on **team-service**, **investment-service**, **user-service**, **auth-service**.

**Manual / Postman:** re-import collection + environment; after register, run **Login** for each persona to refresh `accessToken` variables.

---

*End of report.*
