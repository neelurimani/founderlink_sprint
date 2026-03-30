#!/usr/bin/env bash
# Shared helpers for FounderLink HTTP / Newman E2E scripts.
# shellcheck shell=bash
# When sourced, BASH_SOURCE refers to this file.

_HTTP_E2E_LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HTTP_E2E_ROOT="$(cd "${_HTTP_E2E_LIB_DIR}/../.." && pwd)"

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
RABBIT_MGMT_URL="${RABBIT_MGMT_URL:-http://localhost:15672}"
NOTIFICATION_CONTAINER="${NOTIFICATION_CONTAINER:-founderlink-notification}"
RABBIT_USER="${RABBIT_USER:-guest}"
RABBIT_PASS="${RABBIT_PASS:-guest}"

e2e_die() { echo "ERROR: $*" >&2; exit 1; }

e2e_need_cmd() {
  command -v "$1" >/dev/null 2>&1 || e2e_die "missing command: $1 (install or add to PATH)"
}

e2e_idempotency_key() {
  if command -v uuidgen >/dev/null 2>&1; then
    uuidgen
  else
    openssl rand -hex 16
  fi
}

# JWT payload userId or sub (no signature check; for test automation only).
e2e_jwt_user_id() {
  local token="$1"
  python3 -c "
import sys, json, base64
t = sys.argv[1].split('.')[1]
t += '=' * (-len(t) % 4)
p = json.loads(base64.urlsafe_b64decode(t.encode('ascii')))
print(p.get('userId') or p.get('sub') or '')
" "$token"
}

# Wait until URL returns HTTP 2xx (curl -f).
e2e_wait_http_ok() {
  local url="$1"
  local label="$2"
  local max="${3:-90}"
  local i
  for ((i = 1; i <= max; i++)); do
    if curl -sS -f -o /dev/null "$url" 2>/dev/null; then
      echo "OK: ${label}"
      return 0
    fi
    sleep 1
  done
  e2e_die "${label} not ready after ${max}s: ${url}"
}

e2e_wait_rabbitmq_mgmt() {
  local max="${1:-60}"
  local i
  for ((i = 1; i <= max; i++)); do
    if curl -sS -f -u "${RABBIT_USER}:${RABBIT_PASS}" -o /dev/null "${RABBIT_MGMT_URL}/api/overview" 2>/dev/null; then
      echo "OK: RabbitMQ management API"
      return 0
    fi
    sleep 1
  done
  e2e_die "RabbitMQ management not ready after ${max}s: ${RABBIT_MGMT_URL}"
}

# Run Newman for one collection folder (name must match Postman folder exactly).
e2e_newman_folder() {
  local folder="$1"
  shift
  (
    cd "${HTTP_E2E_ROOT}" || e2e_die "cd HTTP_E2E_ROOT"
    npx --yes newman@6 run postman/FounderLink.postman_collection.json \
      -e postman/FounderLink.postman_environment.json \
      --folder "${folder}" \
      --env-var "gatewayUrl=${GATEWAY_URL}" \
      "$@"
  )
}

# Full collection (all services, same order as Postman tree).
e2e_newman_full_collection() {
  (
    cd "${HTTP_E2E_ROOT}" || e2e_die "cd HTTP_E2E_ROOT"
    npx --yes newman@6 run postman/FounderLink.postman_collection.json \
      -e postman/FounderLink.postman_environment.json \
      --env-var "gatewayUrl=${GATEWAY_URL}" \
      --delay-request "${NEWMAN_DELAY_MS:-80}" \
      "$@"
  )
}

# notification-service with founderlink.email.delivery=log writes one line per send: "[FounderLink email] ..."
e2e_verify_notification_logged_emails() {
  local min="${1:-${MIN_NOTIFICATION_LOG_LINES:-1}}"
  local pause="${2:-${E2E_NOTIFICATION_WAIT_SECONDS:-10}}"
  if [[ "${min}" == "0" ]]; then
    echo "(MIN_NOTIFICATION_LOG_LINES=0 — skipping notification email log assert)"
    return 0
  fi
  command -v docker >/dev/null 2>&1 || e2e_die "docker required to assert notification log lines"
  echo "--- Waiting ${pause}s for async notification emails (log delivery) ---"
  sleep "${pause}"
  docker container inspect "${NOTIFICATION_CONTAINER}" >/dev/null 2>&1 \
    || e2e_die "Docker container ${NOTIFICATION_CONTAINER} not found (start stack with ./scripts/docker-up-all.sh)"
  local n
  n="$(docker logs "${NOTIFICATION_CONTAINER}" 2>&1 | grep -cF '[FounderLink email]' || true)"
  n="${n//[[:space:]]/}"
  [[ "${n}" =~ ^[0-9]+$ ]] || n=0
  [[ "${n}" -ge "${min}" ]] || e2e_die \
    "notification-service: expected at least ${min} log line(s) containing [FounderLink email], got ${n}. Check: docker logs ${NOTIFICATION_CONTAINER}"
  echo "OK: notification-service logged ${n} email line(s) (min ${min})"
}

e2e_verify_rabbitmq_notification_queue() {
  local json
  json="$(curl -sS -f -u "${RABBIT_USER}:${RABBIT_PASS}" "${RABBIT_MGMT_URL}/api/queues")" \
    || e2e_die "RabbitMQ: failed to list queues"
  echo "${json}" | jq -e 'map(.name) | index("notification.queue") != null' >/dev/null 2>&1 \
    || e2e_die "RabbitMQ: queue notification.queue not found (consumers may not have declared it yet — start full stack)"
  echo "OK: RabbitMQ has notification.queue"
}

e2e_verify_rabbitmq_exchange() {
  local json
  json="$(curl -sS -f -u "${RABBIT_USER}:${RABBIT_PASS}" "${RABBIT_MGMT_URL}/api/exchanges/%2F/founderlink.exchange")" \
    || e2e_die "RabbitMQ: exchange founderlink.exchange not found"
  echo "${json}" | jq -e '.name == "founderlink.exchange"' >/dev/null 2>&1 \
    || e2e_die "RabbitMQ: founderlink.exchange missing"
  echo "OK: RabbitMQ has founderlink.exchange"
}
