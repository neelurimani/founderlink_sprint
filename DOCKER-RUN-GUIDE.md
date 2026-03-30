# FounderLink Docker Run Guide (No Orchestration)

This project is Dockerized service-by-service without Docker Compose or Kubernetes.

## 1) Prerequisites

- **Docker daemon running** — Jib and `docker` both need it. Quick check: `docker info` (or `docker version`) must succeed before `./scripts/docker-build-all.sh`.
- Ports available: `3306, 8761, 8888, 8080-8087, 5672, 15672, 9411`

## 2) Build all images (Jib)

Images are built with **[Jib](https://github.com/GoogleContainerTools/jib)** (`jib:dockerBuild`) into the local Docker daemon as `founderlink/<artifactId>:latest` (same tags `docker-up-all.sh` uses). Requires **JDK 17**, **Maven**, and a running Docker daemon.

**Use Jib, not `docker build` on the service `Dockerfile`s.** A typical multi-stage Dockerfile runs Maven *inside* a container: it re-downloads or re-resolves dependencies and compiles the whole reactor on every clean build, which is slow and heavy on network and CPU. **Jib compiles on the host** using your normal `~/.m2` cache, then assembles image layers and loads them into Docker—incremental builds are usually **much faster** (often roughly a minute for all services after the first run, versus many minutes per full Docker build).

From project root:

```bash
./scripts/docker-build-all.sh
```

Equivalent Maven command:

```bash
mvn -DskipTests jib:dockerBuild
```

To rebuild a single module during development:

```bash
mvn -pl auth-service -DskipTests jib:dockerBuild
```

Service `Dockerfile`s are optional legacy references only; **`./scripts/docker-build-all.sh` does not invoke them.**

## 3) One-shot: start everything

From project root (after `./scripts/docker-build-all.sh`):

```bash
./scripts/docker-up-all.sh
```

This starts MySQL, RabbitMQ, Zipkin, then all FounderLink containers in the correct order. Notifications use **log email delivery** by default (search container logs for `[FounderLink email]`). Existing containers with the same names are **started** instead of recreated.

## 4) Start infrastructure containers (manual)

MySQL:

```bash
docker run -d --name founderlink-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=12345678 \
  -e MYSQL_DATABASE=founderlink \
  mysql:8.4
```

RabbitMQ:

```bash
docker run -d --name founderlink-rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:3-management
```

Zipkin (optional, for tracing):

```bash
docker run -d --name founderlink-zipkin -p 9411:9411 openzipkin/zipkin
```

## 5) Start FounderLink services manually (recommended order)

When running **inside Docker**, pass **`EUREKA_SERVER_URL=http://host.docker.internal:8761/eureka/`** (and optionally **`EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`** with the same value). Each app also declares **`eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL:...}`** in its **local** `application.properties` / gateway `application.yml` so the Netflix client always has a bindable property. Without that line, the client often falls back to **`http://localhost:8761`**, which fails inside a container—then only **CONFIG-GLOBAL** (which already had Eureka in its local config) shows in Eureka. Start **config** before other services and give it a few seconds (see `docker-up-all.sh`).

### Eureka (registry server)
```bash
docker run -d --name founderlink-eureka \
  -p 8761:8761 \
  founderlink/eureka:latest
```

### Config Server
Mount the repo `global-config/` folder so YAML changes apply without rebuilding the image (paths: run from project root).
```bash
docker run -d --name founderlink-config \
  -p 8888:8888 \
  --add-host=host.docker.internal:host-gateway \
  -v "$(pwd)/global-config:/config:ro" \
  -e SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS=file:/config \
  -e EUREKA_SERVER_URL=http://host.docker.internal:8761/eureka/ \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  founderlink/config-global:latest
```

### Domain Services
```bash
docker run -d --name founderlink-auth \
  -p 8083:8083 \
  -e CONFIG_SERVER_URL=http://host.docker.internal:8888 \
  -e EUREKA_SERVER_URL=http://host.docker.internal:8761/eureka/ \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  -e MYSQL_HOST=host.docker.internal \
  -e MYSQL_PORT=3306 \
  -e MYSQL_USERNAME=root \
  -e MYSQL_PASSWORD=12345678 \
  -e RABBITMQ_HOST=host.docker.internal \
  -e ZIPKIN_ENDPOINT=http://host.docker.internal:9411/api/v2/spans \
  founderlink/auth-service:latest
```

```bash
docker run -d --name founderlink-user \
  -p 8081:8081 \
  -e CONFIG_SERVER_URL=http://host.docker.internal:8888 \
  -e EUREKA_SERVER_URL=http://host.docker.internal:8761/eureka/ \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  -e MYSQL_HOST=host.docker.internal \
  -e MYSQL_PORT=3306 \
  -e MYSQL_USERNAME=root \
  -e MYSQL_PASSWORD=12345678 \
  -e RABBITMQ_HOST=host.docker.internal \
  -e ZIPKIN_ENDPOINT=http://host.docker.internal:9411/api/v2/spans \
  founderlink/user-service:latest
```

```bash
docker run -d --name founderlink-startup \
  -p 8082:8082 \
  -e CONFIG_SERVER_URL=http://host.docker.internal:8888 \
  -e EUREKA_SERVER_URL=http://host.docker.internal:8761/eureka/ \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  -e MYSQL_HOST=host.docker.internal \
  -e MYSQL_PORT=3306 \
  -e MYSQL_USERNAME=root \
  -e MYSQL_PASSWORD=12345678 \
  -e RABBITMQ_HOST=host.docker.internal \
  -e ZIPKIN_ENDPOINT=http://host.docker.internal:9411/api/v2/spans \
  founderlink/startup-service:latest
```

```bash
docker run -d --name founderlink-investment \
  -p 8084:8084 \
  -e CONFIG_SERVER_URL=http://host.docker.internal:8888 \
  -e EUREKA_SERVER_URL=http://host.docker.internal:8761/eureka/ \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  -e MYSQL_HOST=host.docker.internal \
  -e MYSQL_PORT=3306 \
  -e MYSQL_USERNAME=root \
  -e MYSQL_PASSWORD=12345678 \
  -e RABBITMQ_HOST=host.docker.internal \
  -e ZIPKIN_ENDPOINT=http://host.docker.internal:9411/api/v2/spans \
  founderlink/investment-service:latest
```

```bash
docker run -d --name founderlink-messaging \
  -p 8085:8085 \
  -e CONFIG_SERVER_URL=http://host.docker.internal:8888 \
  -e EUREKA_SERVER_URL=http://host.docker.internal:8761/eureka/ \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  -e MYSQL_HOST=host.docker.internal \
  -e MYSQL_PORT=3306 \
  -e MYSQL_USERNAME=root \
  -e MYSQL_PASSWORD=12345678 \
  -e RABBITMQ_HOST=host.docker.internal \
  -e ZIPKIN_ENDPOINT=http://host.docker.internal:9411/api/v2/spans \
  founderlink/messaging-service:latest
```

```bash
docker run -d --name founderlink-notification \
  -p 8086:8086 \
  -e CONFIG_SERVER_URL=http://host.docker.internal:8888 \
  -e EUREKA_SERVER_URL=http://host.docker.internal:8761/eureka/ \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  -e MYSQL_HOST=host.docker.internal \
  -e MYSQL_PORT=3306 \
  -e MYSQL_USERNAME=root \
  -e MYSQL_PASSWORD=12345678 \
  -e RABBITMQ_HOST=host.docker.internal \
  -e FOUNDERLINK_EMAIL_ENABLED=true \
  -e FOUNDERLINK_EMAIL_DELIVERY=log \
  -e ZIPKIN_ENDPOINT=http://host.docker.internal:9411/api/v2/spans \
  founderlink/notification-service:latest
```

```bash
docker run -d --name founderlink-analytics \
  -p 8087:8087 \
  -e CONFIG_SERVER_URL=http://host.docker.internal:8888 \
  -e EUREKA_SERVER_URL=http://host.docker.internal:8761/eureka/ \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  -e MYSQL_HOST=host.docker.internal \
  -e MYSQL_PORT=3306 \
  -e MYSQL_USERNAME=root \
  -e MYSQL_PASSWORD=12345678 \
  -e RABBITMQ_HOST=host.docker.internal \
  -e ZIPKIN_ENDPOINT=http://host.docker.internal:9411/api/v2/spans \
  founderlink/analytics-service:latest
```

### API Gateway (start last)
```bash
docker run -d --name founderlink-gateway \
  -p 8080:8080 \
  -e CONFIG_SERVER_URL=http://host.docker.internal:8888 \
  -e EUREKA_SERVER_URL=http://host.docker.internal:8761/eureka/ \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  -e ZIPKIN_ENDPOINT=http://host.docker.internal:9411/api/v2/spans \
  founderlink/api-gateway:latest
```

## 6) Verify

- Headless gateway smoke (Postman collection via Newman, needs Node/npx): `./scripts/run-postman-e2e.sh`
- Eureka: `http://localhost:8761`
- Gateway routes: `http://localhost:8080/gateway/routes` (requires JWT in current config)
- Zipkin: `http://localhost:9411`
- RabbitMQ UI: `http://localhost:15672` (`guest` / `guest`)
- Notification emails (dev): `docker logs founderlink-notification 2>&1 | grep FounderLink`

## 7) Stop and clean

```bash
docker rm -f \
  founderlink-gateway founderlink-analytics founderlink-notification founderlink-messaging \
  founderlink-investment founderlink-startup founderlink-user founderlink-auth \
  founderlink-config founderlink-eureka \
  founderlink-rabbitmq founderlink-mysql founderlink-zipkin
```

## 8) Troubleshooting: `docker.sock` / “failed to connect to the docker API”

If you see:

`dial unix .../.docker/run/docker.sock: connect: no such file or directory`

then **no Docker daemon is listening** on the socket the CLI is using. That is not a FounderLink or Jib bug.

**On macOS (Docker Desktop):**

1. Open **Docker Desktop** from Applications and wait until it says **Docker is running** (whale icon steady in the menu bar).
2. Run `docker info` again. If it still fails, quit Docker Desktop fully and start it again.
3. Ensure you are not pointing at a remote or broken context: `docker context ls` and `docker context use default` if needed.

**Jib** (`jib:dockerBuild`) loads images with the same Docker CLI/daemon as `docker rm`; both require a running engine. After `docker info` works, rerun `./scripts/docker-build-all.sh`.
