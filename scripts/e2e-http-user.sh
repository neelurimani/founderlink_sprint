#!/usr/bin/env bash
# Newman: User Service folder. Run after Auth so tokens exist in the same Newman env (ephemeral).
# For a greenfield run, use e2e-full-stack-http.sh instead.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=scripts/lib/http-e2e-common.sh
source "${ROOT}/scripts/lib/http-e2e-common.sh"
export GATEWAY_URL
e2e_need_cmd curl
[[ "${SKIP_WAIT:-0}" == "1" ]] || e2e_wait_http_ok "${GATEWAY_URL}/actuator/health" "API Gateway" 90
e2e_newman_folder "User Service" "$@"
