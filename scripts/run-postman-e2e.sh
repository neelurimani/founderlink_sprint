#!/usr/bin/env bash
# Newman only: full Postman collection through the gateway (no notification log/Rabbit asserts).
# For waits + notification log + RabbitMQ verification, use: ./scripts/e2e-full-stack-http.sh
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
export GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
exec npx --yes newman@6 run postman/FounderLink.postman_collection.json \
  -e postman/FounderLink.postman_environment.json \
  --env-var "gatewayUrl=${GATEWAY_URL}" \
  --delay-request "${NEWMAN_DELAY_MS:-80}" \
  --reporters cli "$@"
