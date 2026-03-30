#!/usr/bin/env bash
# Legacy name: asserts notification-service logged outbound emails (log delivery; MailHog removed).
# Usage: MIN_NOTIFICATION_LOG_LINES=1 E2E_NOTIFICATION_WAIT_SECONDS=10 ./scripts/e2e-verify-mailhog.sh
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=scripts/lib/http-e2e-common.sh
source "${ROOT}/scripts/lib/http-e2e-common.sh"

command -v docker >/dev/null 2>&1 || e2e_die "missing docker"
e2e_verify_notification_logged_emails "${MIN_NOTIFICATION_LOG_LINES:-${MIN_MESSAGES:-1}}" "${E2E_NOTIFICATION_WAIT_SECONDS:-5}"
