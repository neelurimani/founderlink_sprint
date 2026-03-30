# FounderLink Port Map

## Default Ports

- `eureka` (Eureka registry): `8761` (`EUREKA_SERVER_PORT`; legacy alias `DISCOVERY_SERVER_PORT`)
- `config-global`: `8888`
- `api-gateway`: `8080` (`API_GATEWAY_PORT`)
- `user-service`: `8081` (`USER_SERVICE_PORT`)
- `startup-service`: `8082` (`STARTUP_SERVICE_PORT`)
- `auth-service`: `8083` (`AUTH_SERVICE_PORT`)
- `investment-service`: `8084` (`INVESTMENT_SERVICE_PORT`)
- `team-service`: `8088` (`TEAM_SERVICE_PORT`)
- `messaging-service`: `8085` (`MESSAGING_SERVICE_PORT`)
- `notification-service`: `8086` (`NOTIFICATION_SERVICE_PORT`)
- `analytics-service`: `8087` (`ANALYTICS_SERVICE_PORT`)

## Local development (IntelliJ / host)

Run **Eureka**, **Config**, **API Gateway**, and **all business services** on the host with the ports above. Use **Docker only for infrastructure**: MySQL (`3306`), RabbitMQ (`5672`, `15672`), Zipkin (`9411`) — for example `docker compose up -d` from the project root.

The API Gateway proxies to downstream services using **`localhost`** and these port variables by default (`global-config/api-gateway.yml`). Override **`GATEWAY_DOWNSTREAM_HOST`** only if the gateway runs inside Docker while apps stay on the host (e.g. `host.docker.internal` on Docker Desktop).

## Why this was changed

Each service now uses a service-specific environment variable instead of shared `SERVER_PORT`. This prevents accidental port collisions when `SERVER_PORT` is set globally in terminal/IDE.

## Terminal Utilities

- Show current listeners on all FounderLink ports:
  - `./scripts/ports.sh status`
- Close all listeners on FounderLink ports:
  - `./scripts/ports.sh kill`
