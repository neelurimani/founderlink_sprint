#!/usr/bin/env bash
# Compile all Maven modules (no Docker). Use ./scripts/docker-build-all.sh for Jib images.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"
mvn -DskipTests clean package "$@"
