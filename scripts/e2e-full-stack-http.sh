#!/usr/bin/env bash
# Full-stack HTTP E2E: wait infra → Newman (entire Postman collection) → notification log + RabbitMQ checks.
# Same requests as opening Postman and running the collection; no manual clicking.
#
# Prerequisites: Docker stack (./scripts/docker-up-all.sh), seeded or idempotent registers in collection.
# Tools: curl, jq, Node/npx (Newman), docker (for notification log assert).
#
# Env:
#   GATEWAY_URL, RABBIT_MGMT_URL, RABBIT_USER, RABBIT_PASS, NOTIFICATION_CONTAINER
#   SKIP_INFRA_WAIT=1   — skip gateway / Rabbit waits
#   SKIP_NOTIFICATION_LOG_CHECK=1 — skip "[FounderLink email]" log line count (also: SKIP_MAILHOG_CHECK=1 legacy)
#   SKIP_RABBIT_CHECK=1  — skip RabbitMQ asserts
#   NEWMAN_DELAY_MS     — delay between requests (default 80)
#   E2E_NOTIFICATION_WAIT_SECONDS — seconds to wait before counting log lines (default 10)
#   MIN_NOTIFICATION_LOG_LINES=0  — skip email log minimum count
#   Extra args passed to Newman (e.g. --bail)
#
# Maven (parent reactor only — do not recurse into modules):
#   mvn -N -P http-e2e verify
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=scripts/lib/http-e2e-common.sh
source "${ROOT}/scripts/lib/http-e2e-common.sh"

export GATEWAY_URL
export RABBIT_MGMT_URL="${RABBIT_MGMT_URL:-http://localhost:15672}"

e2e_need_cmd curl
e2e_need_cmd jq
e2e_need_cmd python3
command -v npx >/dev/null 2>&1 || e2e_die "missing npx (install Node.js)"

echo "=== FounderLink full-stack HTTP E2E (Newman + infra checks) ==="
echo "Gateway: ${GATEWAY_URL}"

if [[ "${SKIP_INFRA_WAIT:-0}" != "1" ]]; then
  e2e_wait_http_ok "${GATEWAY_URL}/actuator/health" "API Gateway" 120
  e2e_wait_rabbitmq_mgmt 90
else
  echo "(SKIP_INFRA_WAIT=1 — not waiting for infra)"
fi

echo "--- Newman: full collection ---"
e2e_newman_full_collection "$@"

if [[ "${SKIP_NOTIFICATION_LOG_CHECK:-0}" != "1" ]] && [[ "${SKIP_MAILHOG_CHECK:-0}" != "1" ]]; then
  echo "--- Notification email (log delivery) ---"
  e2e_verify_notification_logged_emails "${MIN_NOTIFICATION_LOG_LINES:-1}" "${E2E_NOTIFICATION_WAIT_SECONDS:-10}"
else
  echo "(skipping notification log assert — SKIP_NOTIFICATION_LOG_CHECK or SKIP_MAILHOG_CHECK)"
fi

if [[ "${SKIP_RABBIT_CHECK:-0}" != "1" ]]; then
  echo "--- RabbitMQ ---"
  e2e_verify_rabbitmq_notification_queue
  e2e_verify_rabbitmq_exchange
else
  echo "(SKIP_RABBIT_CHECK=1 — skipping RabbitMQ asserts)"
fi

echo "=== Full-stack HTTP E2E finished OK ==="
