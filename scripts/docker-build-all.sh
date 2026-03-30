#!/usr/bin/env bash
# Build all FounderLink service images into the local Docker daemon (Jib only — no Dockerfile build).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

echo "Building FounderLink Docker images with Jib (founderlink/<module>:latest)..."
mvn -DskipTests jib:dockerBuild

echo "All images built successfully."
echo "Recreate app containers to run the new JAR (otherwise docker start keeps old layers):"
echo "  RECREATE_APP_CONTAINERS=1 ./scripts/docker-up-all.sh"
