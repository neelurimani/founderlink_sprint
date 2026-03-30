#!/usr/bin/env bash
set -euo pipefail

PORTS=(8761 8888 8080 8081 8082 8083 8084 8085 8086 8087)

usage() {
  cat <<USAGE
Usage: ./scripts/ports.sh <status|kill>

Commands:
  status   Show processes listening on FounderLink ports
  kill     Stop processes listening on FounderLink ports (SIGTERM, then SIGKILL)
USAGE
}

listeners_for_port() {
  local port="$1"
  lsof -nP -iTCP:"$port" -sTCP:LISTEN 2>/dev/null || true
}

status() {
  local found=0
  for port in "${PORTS[@]}"; do
    local out
    out="$(listeners_for_port "$port")"
    if [[ -n "$out" ]]; then
      found=1
      echo "--- port $port ---"
      echo "$out"
    fi
  done

  if [[ "$found" -eq 0 ]]; then
    echo "No listeners on FounderLink ports (${PORTS[*]})."
  fi
}

kill_ports() {
  local pids=()
  local pid

  for port in "${PORTS[@]}"; do
    while IFS= read -r pid; do
      [[ -z "$pid" ]] && continue
      pids+=("$pid")
    done < <(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)
  done

  if [[ "${#pids[@]}" -eq 0 ]]; then
    echo "No listeners on FounderLink ports (${PORTS[*]})."
    return
  fi

  # Deduplicate PIDs (macOS bash 3.2 compatible).
  local deduped=""
  deduped="$(printf '%s\n' "${pids[@]}" | sort -u)"
  pids=()
  while IFS= read -r pid; do
    [[ -z "$pid" ]] && continue
    pids+=("$pid")
  done <<< "$deduped"

  echo "Stopping PIDs: ${pids[*]}"
  kill -15 "${pids[@]}" 2>/dev/null || true
  sleep 1

  local remaining=()
  for pid in "${pids[@]}"; do
    if kill -0 "$pid" 2>/dev/null; then
      remaining+=("$pid")
    fi
  done

  if [[ "${#remaining[@]}" -gt 0 ]]; then
    echo "Force-killing remaining PIDs: ${remaining[*]}"
    kill -9 "${remaining[@]}" 2>/dev/null || true
  fi

  echo "Done. Current status:"
  status
}

main() {
  local cmd="${1:-}"
  case "$cmd" in
    status)
      status
      ;;
    kill)
      kill_ports
      ;;
    *)
      usage
      exit 1
      ;;
  esac
}

main "$@"
