#!/usr/bin/env bash
# Start full FounderLink stack (infra + all services). Requires images from ./docker-build-all.sh
#
# After ./scripts/docker-build-all.sh you MUST use new images. Plain "docker start" keeps old JARs.
#   RECREATE_APP_CONTAINERS=1 ./scripts/docker-up-all.sh
# removes only app containers (not MySQL/RabbitMQ/…) then creates them from the current local images.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Linux: map host.docker.internal to the host gateway (Mac/Win Docker Desktop sets this by default).
DOCKER_HOST_MAP=(--add-host=host.docker.internal:host-gateway)

APP_CONTAINERS=(
  founderlink-gateway
  founderlink-analytics
  founderlink-notification
  founderlink-messaging
  founderlink-investment
  founderlink-startup
  founderlink-user
  founderlink-auth
  founderlink-config
  founderlink-eureka
)

if [[ "${RECREATE_APP_CONTAINERS:-0}" == "1" ]]; then
  echo "=== Recreate app containers (picks up freshly built founderlink/*:latest) ==="
  docker rm -f "${APP_CONTAINERS[@]}" 2>/dev/null || true
fi

ensure_container() {
  local name=$1
  shift
  if docker container inspect "$name" &>/dev/null; then
    docker start "$name" >/dev/null
    echo "Started existing: $name"
  else
    docker run -d --name "$name" "$@"
    echo "Created: $name"
  fi
}

COMMON_SVC_ENV=(
  -e CONFIG_SERVER_URL=http://host.docker.internal:8888
  -e EUREKA_SERVER_URL=http://host.docker.internal:8761/eureka/
  # Spring Boot maps this directly to eureka.client.service-url.defaultZone (fixes Eureka using localhost inside containers)
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/
  -e MYSQL_HOST=host.docker.internal
  -e MYSQL_PORT=3306
  -e MYSQL_USERNAME=root
  -e MYSQL_PASSWORD=12345678
  -e RABBITMQ_HOST=host.docker.internal
  -e SPRING_RABBITMQ_HOST=host.docker.internal
  -e ZIPKIN_ENDPOINT=http://host.docker.internal:9411/api/v2/spans
)

echo "=== Infrastructure ==="
ensure_container founderlink-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=12345678 \
  -e MYSQL_DATABASE=founderlink \
  mysql:8.4

ensure_container founderlink-rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:3-management

ensure_container founderlink-zipkin \
  -p 9411:9411 \
  openzipkin/zipkin

echo "Waiting for MySQL to accept connections..."
for i in {1..45}; do
  if docker exec founderlink-mysql mysqladmin ping -h127.0.0.1 -uroot -p12345678 --silent 2>/dev/null; then
    break
  fi
  sleep 1
  if [[ $i -eq 45 ]]; then
    echo "MySQL did not become ready in time." >&2
    exit 1
  fi
done

echo "=== FounderLink services (order matters) ==="
ensure_container founderlink-eureka -p 8761:8761 "${DOCKER_HOST_MAP[@]}" founderlink/eureka:latest

ensure_container founderlink-config \
  -p 8888:8888 \
  "${DOCKER_HOST_MAP[@]}" \
  -v "${ROOT_DIR}/global-config:/config:ro" \
  -e SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS=file:/config \
  -e EUREKA_SERVER_URL=http://host.docker.internal:8761/eureka/ \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  founderlink/config-global:latest

echo "Waiting for Config Server HTTP..."
for i in {1..60}; do
  if curl -sf "http://localhost:8888/auth-service/dev" >/dev/null 2>&1; then
    break
  fi
  sleep 1
  if [[ $i -eq 60 ]]; then
    echo "Config Server did not become ready; downstream services may miss remote config." >&2
  fi
done

ensure_container founderlink-auth \
  -p 8083:8083 \
  "${DOCKER_HOST_MAP[@]}" \
  "${COMMON_SVC_ENV[@]}" \
  founderlink/auth-service:latest

ensure_container founderlink-user \
  -p 8081:8081 \
  "${DOCKER_HOST_MAP[@]}" \
  "${COMMON_SVC_ENV[@]}" \
  founderlink/user-service:latest

ensure_container founderlink-startup \
  -p 8082:8082 \
  "${DOCKER_HOST_MAP[@]}" \
  "${COMMON_SVC_ENV[@]}" \
  founderlink/startup-service:latest

ensure_container founderlink-investment \
  -p 8084:8084 \
  "${DOCKER_HOST_MAP[@]}" \
  "${COMMON_SVC_ENV[@]}" \
  -e STARTUP_SERVICE_URL=http://host.docker.internal:8082 \
  founderlink/investment-service:latest

ensure_container founderlink-messaging \
  -p 8085:8085 \
  "${DOCKER_HOST_MAP[@]}" \
  "${COMMON_SVC_ENV[@]}" \
  founderlink/messaging-service:latest

ensure_container founderlink-notification \
  -p 8086:8086 \
  "${DOCKER_HOST_MAP[@]}" \
  "${COMMON_SVC_ENV[@]}" \
  -e FOUNDERLINK_EMAIL_ENABLED=true \
  -e FOUNDERLINK_EMAIL_DELIVERY=log \
  founderlink/notification-service:latest

ensure_container founderlink-analytics \
  -p 8087:8087 \
  "${DOCKER_HOST_MAP[@]}" \
  "${COMMON_SVC_ENV[@]}" \
  founderlink/analytics-service:latest

ensure_container founderlink-gateway \
  -p 8080:8080 \
  "${DOCKER_HOST_MAP[@]}" \
  -e CONFIG_SERVER_URL=http://host.docker.internal:8888 \
  -e EUREKA_SERVER_URL=http://host.docker.internal:8761/eureka/ \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  -e ZIPKIN_ENDPOINT=http://host.docker.internal:9411/api/v2/spans \
  founderlink/api-gateway:latest

wait_actuator_http() {
  local url=$1
  local label=$2
  local max=${3:-180}
  local i
  for ((i = 1; i <= max; i++)); do
    if curl -sf "$url" >/dev/null 2>&1; then
      echo "Ready: $label"
      return 0
    fi
    sleep 1
  done
  echo "ERROR: $label not ready after ${max}s ($url) — check: docker logs founderlink-auth / founderlink-gateway" >&2
  return 1
}

echo ""
echo "=== Waiting for JVM services (first boot / amd64-on-arm can take several minutes) ==="
wait_actuator_http "http://localhost:8083/actuator/health" "auth-service" 240
wait_actuator_http "http://localhost:8080/actuator/health" "api-gateway" 120

echo ""
echo "Stack is up. Quick checks:"
echo "  Eureka:    http://localhost:8761"
echo "  Gateway:   http://localhost:8080"
echo "  RabbitMQ:  http://localhost:15672 (guest/guest)"
echo "  Zipkin:    http://localhost:9411"
echo "  (Emails: notification-service logs lines tagged [FounderLink email] — delivery=log)"
