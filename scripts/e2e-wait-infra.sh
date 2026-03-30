#!/usr/bin/env bash
# Block until gateway and RabbitMQ management API respond.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=scripts/lib/http-e2e-common.sh
source "${ROOT}/scripts/lib/http-e2e-common.sh"

export GATEWAY_URL
export RABBIT_MGMT_URL

e2e_need_cmd curl
e2e_wait_http_ok "${GATEWAY_URL}/actuator/health" "API Gateway" 120
e2e_wait_rabbitmq_mgmt 90
echo "All infra endpoints reachable."
