#!/usr/bin/env bash
# Live HTTP E2E for investment APIs via the API gateway (same paths as Postman).
# For the full suite + notification log + RabbitMQ: ./scripts/e2e-full-stack-http.sh
# Prerequisites: stack up, seeded users from Postman env defaults.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
# shellcheck source=scripts/lib/http-e2e-common.sh
source "${ROOT}/scripts/lib/http-e2e-common.sh"

export GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
FOUNDER_EMAIL="${FOUNDER_EMAIL:-local.founder@example.com}"
FOUNDER_PASSWORD="${FOUNDER_PASSWORD:-Password123!}"
INVESTOR_EMAIL="${INVESTOR_EMAIL:-local.investor@example.com}"
INVESTOR_PASSWORD="${INVESTOR_PASSWORD:-Password123!}"
ADMIN_EMAIL="${ADMIN_EMAIL:-local.admin@example.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Password123!}"

e2e_need_cmd curl
e2e_need_cmd jq
e2e_need_cmd python3

login() {
  local email="$1"
  local password="$2"
  local key
  key="$(e2e_idempotency_key)"
  local payload
  payload="$(jq -n --arg e "$email" --arg p "$password" '{email:$e,password:$p}')"
  local resp
  resp="$(curl -sS -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: ${key}" \
    -d "$payload")"
  local body
  body="$(echo "$resp" | sed '$d')"
  local code
  code="$(echo "$resp" | tail -n1)"
  [[ "$code" == "200" ]] || e2e_die "login failed for ${email}: HTTP ${code} body=${body}"
  local tok
  tok="$(echo "$body" | jq -r '.accessToken // empty')"
  [[ -n "$tok" ]] || e2e_die "login response missing accessToken for ${email}"
  echo "$tok"
}

echo "=== Investment HTTP E2E (gateway: ${GATEWAY_URL}) ==="

echo "--- Logins ---"
FOUNDER_TOKEN="$(login "$FOUNDER_EMAIL" "$FOUNDER_PASSWORD")"
INVESTOR_TOKEN="$(login "$INVESTOR_EMAIL" "$INVESTOR_PASSWORD")"
ADMIN_TOKEN="$(login "$ADMIN_EMAIL" "$ADMIN_PASSWORD")"

INVESTOR_UID="$(e2e_jwt_user_id "$INVESTOR_TOKEN")"
[[ -n "$INVESTOR_UID" ]] || e2e_die "could not read user id from investor JWT"

echo "--- Create startup (founder) ---"
CKEY="$(e2e_idempotency_key)"
STARTUP_RESP="$(curl -sS -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/startups" \
  -H "Authorization: Bearer ${FOUNDER_TOKEN}" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: ${CKEY}" \
  -d '{
    "name": "E2E Investment Test Co",
    "description": "HTTP script startup",
    "industry": "FinTech",
    "problemStatement": "Test",
    "solution": "Test",
    "fundingGoal": 50000,
    "stage": "MVP"
  }')"
STARTUP_BODY="$(echo "$STARTUP_RESP" | sed '$d')"
STARTUP_CODE="$(echo "$STARTUP_RESP" | tail -n1)"
[[ "$STARTUP_CODE" == "201" || "$STARTUP_CODE" == "200" ]] || e2e_die "create startup failed: HTTP ${STARTUP_CODE} ${STARTUP_BODY}"
STARTUP_ID="$(echo "$STARTUP_BODY" | jq -r '.id // empty')"
[[ -n "$STARTUP_ID" ]] || e2e_die "create startup response missing id"

echo "--- Create investment (investor) ---"
IKEY="$(e2e_idempotency_key)"
INV_RESP="$(curl -sS -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/investments" \
  -H "Authorization: Bearer ${INVESTOR_TOKEN}" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: ${IKEY}" \
  -d "{\"investorId\":${INVESTOR_UID},\"startupId\":${STARTUP_ID},\"amount\":1000}")"
INV_BODY="$(echo "$INV_RESP" | sed '$d')"
INV_CODE="$(echo "$INV_RESP" | tail -n1)"
[[ "$INV_CODE" == "200" || "$INV_CODE" == "201" ]] || e2e_die "create investment failed: HTTP ${INV_CODE} ${INV_BODY}"
INVESTMENT_ID="$(echo "$INV_BODY" | jq -r '.id // empty')"
[[ -n "$INVESTMENT_ID" ]] || e2e_die "create investment response missing id"

echo "--- GET /api/investments/investor/{id} ---"
G1="$(curl -sS -o /tmp/inv_g1.json -w "%{http_code}" -X GET \
  "${GATEWAY_URL}/api/investments/investor/${INVESTOR_UID}" \
  -H "Authorization: Bearer ${INVESTOR_TOKEN}")"
[[ "$G1" == "200" ]] || e2e_die "GET investor investments failed: HTTP $G1 $(cat /tmp/inv_g1.json)"
jq -e --argjson id "$INVESTMENT_ID" 'map(.id) | index($id) != null' /tmp/inv_g1.json >/dev/null \
  || e2e_die "new investment id not listed under investor"

echo "--- GET /api/investments/startup/{id} (investor) ---"
G2="$(curl -sS -o /tmp/inv_g2.json -w "%{http_code}" -X GET \
  "${GATEWAY_URL}/api/investments/startup/${STARTUP_ID}" \
  -H "Authorization: Bearer ${INVESTOR_TOKEN}")"
[[ "$G2" == "200" ]] || e2e_die "GET startup investments failed: HTTP $G2 $(cat /tmp/inv_g2.json)"

echo "--- PUT /api/investments/{id}/approve (admin) ---"
G3="$(curl -sS -o /tmp/inv_g3.json -w "%{http_code}" -X PUT \
  "${GATEWAY_URL}/api/investments/${INVESTMENT_ID}/approve" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}")"
[[ "$G3" == "200" ]] || e2e_die "approve investment failed: HTTP $G3 $(cat /tmp/inv_g3.json)"
[[ "$(jq -r '.status' /tmp/inv_g3.json)" == "APPROVED" ]] || e2e_die "expected status APPROVED got $(jq -r '.status' /tmp/inv_g3.json)"

echo "=== All investment endpoint checks passed ==="
