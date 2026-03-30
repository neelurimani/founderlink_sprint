# Postman

## What to import (Postman → Import → File)

| File | Import? |
|------|--------|
| **`FounderLink.postman_collection.json`** | Yes — the API collection |
| **`FounderLink.postman_environment.json`** | Yes — variables (`gatewayUrl`, role emails/passwords, tokens, `startupIndustry`, …) |

Do **not** use Postman Import for `accounts-by-role.md`. That file is **documentation only**. Postman only accepts formats like Collection v2.1 and Environment JSON.

## After import

1. Select environment **FounderLink Local** (top right).
2. **Idempotency:** The collection pre-request script sets a new **GUID** in `idempotencyKey` for each request. The gateway **requires** `Idempotency-Key` on **POST** under `/api/**` **except** `/api/auth/**` (register/login/refresh work without it; the header is still sent but ignored for auth routes).
3. **Public startup catalog:** **List Startups (paginated)** and **List Startups by Industry (public)** use **Auth → No Auth** so you can call them without logging in. They return **APPROVED** startups only (for non-admin). Set **`startupIndustry`** (default `FinTech`) for the industry filter.
4. **Register first** (once per role, in order): **Register (FOUNDER)** → **Register (INVESTOR)** → **Register (COFOUNDER)** → **Register (ADMIN)**. Register returns **`message`**, **`userId`**, **`email`** only — **no JWT**. Each uses the same vars as login (`founderName` / `founderEmail` / `founderPassword`, etc.). Defaults are `local.*@example.com` and `Password123!`. **ADMIN** needs `adminRegistrationCode` (must match `auth-service` config).
5. **Login:** **Login (Founder)** / **Login (Investor)** / etc. — returns **`accessToken`** and **`refreshToken`**. Register/Login test scripts store **`founderAuthUserId`**, **`investorAuthUserId`**, **`cofounderAuthUserId`**, **`adminAuthUserId`** from the JWT.
6. **Startups:** **Create Startup** uses **`founderAccessToken`**. **Approve Startup (ADMIN)** uses body `{"status":"APPROVED"}` (or `REJECTED`). Run **Login (Admin)** before approve.
7. **Create Investment** uses **`investorAccessToken`** and **`investorAuthUserId`** (run **Login (Investor)** before that call; the startup must be **APPROVED**).

### Wipe DB + reset Postman for a clean run

From the repo root (Docker MySQL running):

```bash
./scripts/mysql-nuke-founderlink.sh
# restart app containers so services reconnect cleanly
```

Then in Postman: **re-import** `FounderLink.postman_environment.json` (or clear tokens and all `*AuthUserId`, `startupId`, `investmentId` manually).

See **`accounts-by-role.md`** if you need the default local passwords by role.

## Headless (Newman)

```bash
./scripts/run-postman-e2e.sh
```

Uses the same two JSON files above.
