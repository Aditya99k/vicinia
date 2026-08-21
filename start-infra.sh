#!/usr/bin/env bash
#
# Starts the full Vicinia local dev stack, in dependency order:
#   1. Docker infra: Postgres, Redis, Redpanda (+ their web UIs)
#   2. Spring Boot services, in the order each depends on the last
#
# Usage: ./start-infra.sh
# Logs land in ./logs/<service>.log; PIDs are tracked in ./.pids/ so
# stop-infra.sh can shut everything back down cleanly.
#
# ── Maintenance note ────────────────────────────────────────────────
# When a new backend module is scaffolded (per BUILD_TRACKER.md's stage
# order), add one line to the SERVICES array below, in the position that
# matches its real dependency order. Nothing else in this script needs
# to change.
# ─────────────────────────────────────────────────────────────────────

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"
LOG_DIR="$SCRIPT_DIR/logs"
PID_DIR="$SCRIPT_DIR/.pids"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.infra.yml"

mkdir -p "$LOG_DIR" "$PID_DIR"

# name : module directory : port
SERVICES=(
  "discovery-server:discovery-server:8761"
  "config-server:config-server:8888"
  "api-gateway:api-gateway:8080"
  "auth-service:auth-service:8081"
  "user-service:user-service:8082"
)

# ── Colors ─────────────────────────────────────────────────────────
BOLD=$'\033[1m'; DIM=$'\033[2m'; RESET=$'\033[0m'
GREEN=$'\033[0;32m'; YELLOW=$'\033[0;33m'; RED=$'\033[0;31m'; CYAN=$'\033[0;36m'

STEP=0
section() {
  STEP=$((STEP+1))
  echo
  printf "${BOLD}${CYAN}── %d. %s ──${RESET}\n" "$STEP" "$1"
}

ok()   { printf "  ${GREEN}✔${RESET} %s\n" "$1"; }
skip() { printf "  ${YELLOW}●${RESET} %s ${DIM}%s${RESET}\n" "$1" "$2"; }
fail() { printf "  ${RED}✖${RESET} %s\n" "$1"; }

wait_for_http() {
  local name="$1" url="$2" timeout="${3:-120}" waited=0
  printf "  ${DIM}waiting for %s${RESET}" "$name"
  while ! curl -fsS -o /dev/null "$url" 2>/dev/null; do
    if [ "$waited" -ge "$timeout" ]; then
      printf "\n"
      fail "$name did not become healthy within ${timeout}s — see logs/$name.log"
      return 1
    fi
    printf "."
    sleep 2
    waited=$((waited+2))
  done
  printf "\n"
  ok "$name  ${DIM}$url${RESET}"
  return 0
}

wait_for_container_health() {
  local container="$1" timeout="${2:-60}" waited=0 status
  printf "  ${DIM}waiting for %s${RESET}" "$container"
  while true; do
    status=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "missing")
    if [ "$status" = "healthy" ]; then
      printf "\n"
      ok "$container  ${DIM}healthy${RESET}"
      return 0
    fi
    if [ "$waited" -ge "$timeout" ]; then
      printf "\n"
      fail "$container not healthy after ${timeout}s (status: $status)"
      return 1
    fi
    printf "."
    sleep 2
    waited=$((waited+2))
  done
}

# ── 0. Banner ──────────────────────────────────────────────────────
printf "${BOLD}${CYAN}"
cat <<'EOF'

  ╔══════════════════════════════════════╗
  ║            VICINIA · start           ║
  ╚══════════════════════════════════════╝
EOF
printf "${RESET}"

# ── 1. Docker daemon ───────────────────────────────────────────────
section "Docker daemon"
if docker info >/dev/null 2>&1; then
  ok "already running"
else
  skip "not running" "launching Docker Desktop…"
  open -a Docker
  waited=0
  until docker info >/dev/null 2>&1; do
    if [ "$waited" -ge 90 ]; then fail "Docker did not start within 90s — start it manually and re-run"; exit 1; fi
    sleep 2; waited=$((waited+2))
  done
  ok "Docker is up"
fi

# ── 2. Infra containers ────────────────────────────────────────────
section "Infra containers (Postgres, Redis, Redpanda)"
docker compose -f "$COMPOSE_FILE" up -d 2>&1 | sed 's/^/  /'
wait_for_container_health vicinia-postgres 60 || exit 1
wait_for_container_health vicinia-redis 60 || exit 1
wait_for_container_health vicinia-redpanda 60 || exit 1

# ── 3. Build ────────────────────────────────────────────────────────
section "Build (mvn install — ensures every service builds against common's latest code)"
if ( cd "$BACKEND_DIR" && mvn -q clean install -DskipTests ) > "$LOG_DIR/build.log" 2>&1; then
  ok "build succeeded"
else
  fail "build failed — see logs/build.log"
  exit 1
fi

# ── 4. Spring Boot services, in order ──────────────────────────────
section "Application services"
for entry in "${SERVICES[@]}"; do
  IFS=':' read -r name dir port <<< "$entry"
  health_url="http://localhost:${port}/actuator/health"

  if curl -fsS -o /dev/null "$health_url" 2>/dev/null; then
    skip "$name" "already running on :$port"
    continue
  fi

  (
    cd "$BACKEND_DIR/$dir" || exit 1
    nohup mvn -q spring-boot:run > "$LOG_DIR/$name.log" 2>&1 &
    echo $! > "$PID_DIR/$name.pid"
  )

  wait_for_http "$name" "$health_url" 120 || exit 1
done

# ── 5. Eureka registry snapshot ─────────────────────────────────────
# A service's /actuator/health can report UP slightly before its Eureka
# registration has propagated, so retry briefly rather than checking once.
section "Eureka registry"
apps=""
for _ in 1 2 3 4 5; do
  apps=$(curl -s http://localhost:8761/eureka/apps -H "Accept: application/json" 2>/dev/null \
    | grep -o '"name":"[^"]*"' | sed 's/"name":"//;s/"//' | sort -u | grep -v '^MyOwn$')
  [ -n "$apps" ] && break
  sleep 2
done
if [ -n "$apps" ]; then
  echo "$apps" | sed 's/^/    • /'
else
  printf "  ${DIM}(nothing registered yet — give it a few seconds)${RESET}\n"
fi

# ── 6. Summary ───────────────────────────────────────────────────────
printf "\n${BOLD}${GREEN}"
cat <<'EOF'
  ╔══════════════════════════════════════════════════════╗
  ║                 Vicinia is up                        ║
  ╚══════════════════════════════════════════════════════╝
EOF
printf "${RESET}\n"

printf "  ${BOLD}%-18s${RESET} %s\n" "discovery-server" "http://localhost:8761"
printf "  ${BOLD}%-18s${RESET} %s\n" "config-server" "http://localhost:8888"
printf "  ${BOLD}%-18s${RESET} %s\n" "api-gateway" "http://localhost:8080"
printf "  ${BOLD}%-18s${RESET} %s\n" "auth-service" "http://localhost:8081"
printf "  ${BOLD}%-18s${RESET} %s\n" "user-service" "http://localhost:8082"
echo
printf "  ${BOLD}%-18s${RESET} %s\n" "postgres" "localhost:5432  (user/pass: vicinia/vicinia)"
printf "  ${BOLD}%-18s${RESET} %s\n" "redis" "localhost:6379"
printf "  ${BOLD}%-18s${RESET} %s\n" "redis-commander" "http://localhost:9191"
printf "  ${BOLD}%-18s${RESET} %s\n" "redpanda" "localhost:9092"
printf "  ${BOLD}%-18s${RESET} %s\n" "redpanda-console" "http://localhost:9093"
echo
printf "  ${DIM}Logs:  ./logs/*.log${RESET}\n"
printf "  ${DIM}Stop:  ./stop-infra.sh${RESET}\n\n"
