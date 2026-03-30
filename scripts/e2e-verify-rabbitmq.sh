#!/usr/bin/env bash
# Assert notification.queue and founderlink.exchange exist (RabbitMQ management API).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=scripts/lib/http-e2e-common.sh
source "${ROOT}/scripts/lib/http-e2e-common.sh"

e2e_need_cmd curl
e2e_need_cmd jq
e2e_verify_rabbitmq_notification_queue
e2e_verify_rabbitmq_exchange
