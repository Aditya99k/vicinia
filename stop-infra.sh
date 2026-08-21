#!/usr/bin/env bash
#
# Stops everything start-infra.sh started: tracked Spring Boot processes
# first, then the Docker infra containers.
#
# Usage: ./stop-infra.sh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_DIR="$SCRIPT_DIR/.pids"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.infra.yml"

BOLD=$'\033[1m'; DIM=$'\033[2m'; RESET=$'\033[0m'
GREEN=$'\033[0;32m'; YELLOW=$'\033[0;33m'; RED=$'\033[0;31m'; CYAN=$'\033[0;36m'

STEP=0
section() {
  STEP=$((STEP+1))
  echo
  printf "${BOLD}${CYAN}── %d. %s ──${RESET}\n" "$STEP" "$1"
}
ok()   { printf "  ${GREEN}✔${RESET} %s\n" "$1"; }
skip() { printf "  ${YELLOW}●${RESET} %s\n" "$1"; }

printf "${BOLD}${CYAN}"
cat <<'EOF'

  ╔══════════════════════════════════════╗
  ║             VICINIA · stop            ║
  ╚══════════════════════════════════════╝
EOF
printf "${RESET}"

section "Application services"
if [ -d "$PID_DIR" ] && [ -n "$(ls -A "$PID_DIR" 2>/dev/null)" ]; then
  for pidfile in "$PID_DIR"/*.pid; do
    [ -e "$pidfile" ] || continue
    name=$(basename "$pidfile" .pid)
    pid=$(cat "$pidfile" 2>/dev/null || echo "")

    if [ -z "$pid" ] || ! kill -0 "$pid" 2>/dev/null; then
      skip "$name  ${DIM}not running${RESET}"
      rm -f "$pidfile"
      continue
    fi

    # sanity check: only kill it if it still looks like our mvn/java process,
    # in case the PID got reused by an unrelated process since we recorded it
    if ! ps -p "$pid" -o command= 2>/dev/null | grep -qE "spring-boot:run|java"; then
      skip "$name  ${DIM}PID $pid no longer looks like our process — leaving it alone${RESET}"
      rm -f "$pidfile"
      continue
    fi

    kill "$pid" 2>/dev/null || true
    for _ in 1 2 3 4 5; do
      kill -0 "$pid" 2>/dev/null || break
      sleep 1
    done
    if kill -0 "$pid" 2>/dev/null; then
      kill -9 "$pid" 2>/dev/null || true
      ok "$name  ${DIM}(force killed)${RESET}"
    else
      ok "$name"
    fi
    rm -f "$pidfile"
  done
else
  skip "no tracked services found"
fi

section "Infra containers"
docker compose -f "$COMPOSE_FILE" down 2>&1 | sed 's/^/  /'

printf "\n${BOLD}${GREEN}Vicinia is stopped.${RESET}\n\n"
