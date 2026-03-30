#!/usr/bin/env bash
# Drop all FounderLink service schemas on Docker MySQL (founderlink-mysql).
# Next service starts recreate empty DBs via createDatabaseIfNotExist / JPA.
set -euo pipefail

ROOT_PW="${MYSQL_ROOT_PASSWORD:-12345678}"
CONTAINER="${MYSQL_CONTAINER:-founderlink-mysql}"

if ! docker container inspect "$CONTAINER" &>/dev/null; then
  echo "Container not running: $CONTAINER" >&2
  exit 1
fi

docker exec -i "$CONTAINER" mysql -uroot -p"$ROOT_PW" <<'SQL'
DROP DATABASE IF EXISTS auth_service;
DROP DATABASE IF EXISTS user_service;
DROP DATABASE IF EXISTS startup_service;
DROP DATABASE IF EXISTS investment_service;
DROP DATABASE IF EXISTS team_service;
DROP DATABASE IF EXISTS messaging_service;
DROP DATABASE IF EXISTS notification_service;
DROP DATABASE IF EXISTS analytics_service;
DROP DATABASE IF EXISTS founderlink;
SQL

echo "MySQL: dropped FounderLink service databases (and founderlink if present)."
