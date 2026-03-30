#!/usr/bin/env bash
# Quick checks when you still see docker.sock / 403 / 502 after "fixing" code.
set -u

echo "=== 1) Docker daemon ==="
if docker info >/dev/null 2>&1; then
  echo "OK: Docker API reachable"
else
  echo "FAIL: Start Docker Desktop (or your engine), then retry."
  exit 1
fi

echo ""
echo "=== 2) FounderLink containers (running image) ==="
docker ps -a --filter name=founderlink --format 'table {{.Names}}\t{{.Status}}\t{{.Image}}' 2>/dev/null || true

echo ""
echo "=== 3) HTTP smoke (host) ==="
for url in \
  "http://localhost:8080/actuator/health" \
  "http://localhost:8761"
do
  code="000"
  code=$(curl -sS -o /dev/null -w "%{http_code}" --max-time 3 "$url" 2>/dev/null) || code="000"
  echo "  ${code}  ${url}"
done

echo ""
echo "=== What to do if errors persist ==="
echo "  A) Code changed but containers are old: rebuild images and RECREATE containers:"
echo "       ./scripts/docker-nuke-rebuild-up.sh"
echo "     (or: ./scripts/docker-build-all.sh then remove app containers and ./scripts/docker-up-all.sh)"
echo "  B) docker.sock / 'no such file': Docker was stopped — start it, then re-run docker/Jib."
echo "  C) E2E notification log 0: need Create Investment 200; check docker logs founderlink-notification for [FounderLink email]"
echo ""
