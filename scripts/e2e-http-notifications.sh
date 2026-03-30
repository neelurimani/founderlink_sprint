#!/usr/bin/env bash
# Newman: Notifications Service folder.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=scripts/lib/http-e2e-common.sh
source "${ROOT}/scripts/lib/http-e2e-common.sh"
export GATEWAY_URL
e2e_need_cmd curl
[[ "${SKIP_WAIT:-0}" == "1" ]] || e2e_wait_http_ok "${GATEWAY_URL}/actuator/health" "API Gateway" 90
e2e_newman_folder "Notifications Service" "$@"
