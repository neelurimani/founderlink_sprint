#!/usr/bin/env bash
# Run the entire Postman collection + notification log + RabbitMQ verification (recommended one-shot E2E).
# Alias for e2e-full-stack-http.sh
exec "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/e2e-full-stack-http.sh" "$@"
