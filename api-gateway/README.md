# FounderLink API Gateway

A universal, configurable API gateway for routing traffic between clients and FounderLink microservices.

## Features

- Path-based routing (`/api/users/**` -> `http://localhost:8081/**`)
- Prefix stripping toggle per route
- API key authentication (`X-API-Key`)
- In-memory per-client rate limiting
- Actuator health/metrics endpoints
- Runtime route visibility at `GET /gateway/routes`

## Configuration

Update routes and security in the config server file:

`global-config/api-gateway.yml` (repository root; served by Spring Cloud Config)

Local fallback values also exist in `src/main/resources/application.yml`.

```yaml
gateway:
  security:
    api-key-header: X-API-Key
    valid-api-keys:
      - founderlink-local-key
  rate-limit:
    requests-per-minute: 120
  routes:
    - id: user-service
      path-prefix: /api/users
      strip-path-prefix: true
      target-base-uri: http://localhost:8081/users
```

## Run

```bash
./mvnw spring-boot:run
```

If config server runs on a different URL:

```bash
CONFIG_SERVER_URL=http://localhost:8888 ./mvnw spring-boot:run
```

## Example Request

```bash
curl -H "X-API-Key: founderlink-local-key" \
  "http://localhost:8080/api/users/1"
```
