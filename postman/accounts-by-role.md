# Account reference (Postman vs database seeds)

## Postman defaults (`FounderLink Local`)

The collection **does not** use SQL seed users. Use **Register (ROLE)** once per email, then **Login (ROLE)** with the **same** environment variables.

JWT-derived ids (filled by test scripts): `founderAuthUserId`, `investorAuthUserId`, `cofounderAuthUserId`, `adminAuthUserId`, plus `authUserId` synced from the current `accessToken` before each request.

| Role | Variables | Default email | Default password |
|------|-----------|---------------|------------------|
| FOUNDER | `founderName`, `founderEmail`, `founderPassword` | local.founder@example.com | Password123! |
| INVESTOR | `investorName`, `investorEmail`, `investorPassword` | local.investor@example.com | Password123! |
| COFOUNDER | `cofounderName`, `cofounderEmail`, `cofounderPassword` | local.cofounder@example.com | Password123! |
| ADMIN | `adminName`, `adminEmail`, `adminPassword` + `adminRegistrationCode` | local.admin@example.com | Password123! |

### Full reset (DB + cache + Postman)

1. `./scripts/mysql-nuke-founderlink.sh`
2. `docker exec founderlink-redis redis-cli FLUSHALL`
3. Restart microservice containers so they recreate schemas.
4. Re-import `FounderLink.postman_environment.json` (clears tokens and ids).

If registration returns “email already exists”, change the emails in the environment (e.g. add `+test1` before `@`) or clear those rows in `auth_service`.

## Optional SQL seed users (not used by Postman)

Some services ship `data.sql` with users like `seed.founder@example.com` / `password`. Those hashes may not match your expectations, which often shows up as **invalid credentials**. Prefer the Postman flow above instead of relying on seeds.
