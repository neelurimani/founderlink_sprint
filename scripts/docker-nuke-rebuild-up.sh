#!/usr/bin/env bash
# Remove FounderLink containers + app images, prune unused volumes, Jib rebuild, start full stack.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "=== 1) Remove FounderLink containers (MySQL/Redis/etc. data in container layers is deleted) ==="
docker rm -f \
  founderlink-gateway founderlink-analytics founderlink-notification founderlink-messaging \
  founderlink-investment founderlink-startup founderlink-user founderlink-auth \
  founderlink-config founderlink-eureka \
  founderlink-rabbitmq founderlink-mysql founderlink-zipkin 2>/dev/null || true

echo "=== 2) Remove founderlink/* images ==="
if imgs=$(docker images 'founderlink/*' -q 2>/dev/null) && [ -n "$imgs" ]; then
  # shellcheck disable=SC2086
  docker rmi -f $imgs 2>/dev/null || true
fi

echo "=== 3) Prune unused Docker volumes (orphaned DB volumes) ==="
docker volume prune -f

echo "=== 4) Jib build (all services) ==="
mvn -DskipTests jib:dockerBuild

echo "=== 5) Deploy stack ==="
exec ./scripts/docker-up-all.sh
