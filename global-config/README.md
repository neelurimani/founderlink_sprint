# FounderLink central configuration

Spring Cloud Config **native** backend reads these files. Naming: `{application}.yml` plus shared `application.yml` and `application-{profile}.yml`.

- **In Docker**: `docker-up-all.sh` mounts this directory at `/config` so you can edit YAML and restart only `founderlink-config` (no image rebuild).
- **Local JAR / Jib**: `config-global` copies this folder into the classpath at build time (`mvn package` / `jib:dockerBuild`).

Clients import via `spring.config.import=optional:configserver:…` and `spring.application.name` (e.g. `auth-service` → `auth-service.yml`).
