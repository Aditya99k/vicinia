#!/usr/bin/env bash
#
# Starts the full Vicinia local dev stack, in dependency order:
#   1. Docker infra: Postgres, Redis, Redpanda (+ their web UIs)
#   2. Spring Boot services, in the order each depends on the last
#   3. frontend/customer-app (Vite, bound to 0.0.0.0 so it's reachable
#      from a phone on the same Wi-Fi too)
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
ENV_FILE="$SCRIPT_DIR/.env"

mkdir -p "$LOG_DIR" "$PID_DIR"

# Every service reads secrets from the environment, not from committed
# YAML (see docker-compose.infra.yml, config-repo/application.yml, and
# each service's application.yml). All of them must be present in .env.
REQUIRED_ENV_VARS=(POSTGRES_USER POSTGRES_PASSWORD POSTGRES_DB VICINIA_INTERNAL_SECRET VICINIA_JWT_SECRET)

# name : module directory : port
SERVICES=(
  "discovery-server:discovery-server:8761"
  "config-server:config-server:8888"
  "api-gateway:api-gateway:8080"
  "auth-service:auth-service:8081"
  "user-service:user-service:8082"
  "merchant-service:merchant-service:8083"
  "catalog-service:catalog-service:8084"
  "inventory-service:inventory-service:8085"
  "cart-service:cart-service:8086"
  "coupon-service:coupon-service:8087"
  "payment-service:payment-service:8088"
  "order-service:order-service:8089"
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

# ── 1. Secrets ───────────────────────────────────────────────────────
section "Secrets (.env)"
if [ ! -f "$ENV_FILE" ]; then
  fail ".env not found — copy .env.example to .env and fill in real values:"
  printf "      ${DIM}cp .env.example .env${RESET}\n"
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

missing=()
for var in "${REQUIRED_ENV_VARS[@]}"; do
  if [ -z "${!var:-}" ]; then
    missing+=("$var")
  fi
done
if [ "${#missing[@]}" -gt 0 ]; then
  fail "Missing values in .env for: ${missing[*]}"
  printf "      ${DIM}see .env.example for how to generate each one${RESET}\n"
  exit 1
fi
ok ".env loaded — ${#REQUIRED_ENV_VARS[@]} variables exported for Docker Compose and every service"

# ── 2. Docker daemon ───────────────────────────────────────────────
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

# ── 3. Infra containers ────────────────────────────────────────────
section "Infra containers (Postgres, Redis, Redpanda, MongoDB)"
docker compose -f "$COMPOSE_FILE" up -d 2>&1 | sed 's/^/  /'
wait_for_container_health vicinia-postgres 60 || exit 1
wait_for_container_health vicinia-redis 60 || exit 1
wait_for_container_health vicinia-redpanda 60 || exit 1
wait_for_container_health vicinia-mongo 60 || exit 1

# ── 4. Build ────────────────────────────────────────────────────────
section "Build (mvn install — ensures every service builds against common's latest code)"
if ( cd "$BACKEND_DIR" && mvn -q clean install -DskipTests ) > "$LOG_DIR/build.log" 2>&1; then
  ok "build succeeded"
else
  fail "build failed — see logs/build.log"
  exit 1
fi

# ── 5. Spring Boot services, in order ──────────────────────────────
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

# ── 6. Frontend (customer-app) ──────────────────────────────────────
section "Frontend (customer-app)"
FRONTEND_DIR="$SCRIPT_DIR/frontend/customer-app"
FRONTEND_URL="http://localhost:5173"

if curl -fsS -o /dev/null "$FRONTEND_URL" 2>/dev/null; then
  skip "customer-app" "already running on :5173"
else
  if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    printf "  ${DIM}installing frontend dependencies (first run)…${RESET}\n"
    if ! ( cd "$FRONTEND_DIR" && npm install ) > "$LOG_DIR/customer-app-install.log" 2>&1; then
      fail "npm install failed — see logs/customer-app-install.log"
      exit 1
    fi
  fi

  (
    cd "$FRONTEND_DIR" || exit 1
    # Runs the local vite binary directly rather than `npm run dev` — npm
    # wraps it in its own process that doesn't reliably forward SIGTERM to
    # the actual vite/node child, which left an orphaned server behind the
    # first time this was tried. Tracking vite's own PID means stop-infra.sh
    # kills the real process, not a wrapper around it.
    # --host binds 0.0.0.0, not just localhost, so it's reachable from a
    # phone on the same Wi-Fi too — see src/api/client.js for how the app
    # then finds the gateway regardless of which host loaded the page.
    nohup ./node_modules/.bin/vite --host > "$LOG_DIR/customer-app.log" 2>&1 &
    echo $! > "$PID_DIR/customer-app.pid"
  )

  wait_for_http "customer-app" "$FRONTEND_URL" 60 || exit 1
fi

# ── 7. Eureka registry snapshot ─────────────────────────────────────
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

# ── 8. Summary ───────────────────────────────────────────────────────
printf "\n${BOLD}${GREEN}"
cat <<'EOF'
  ╔══════════════════════════════════════════════════════╗
  ║                 Vicinia is up                        ║
  ╚══════════════════════════════════════════════════════╝
EOF
printf "${RESET}\n"

for entry in "${SERVICES[@]}"; do
  IFS=':' read -r name dir port <<< "$entry"
  printf "  ${BOLD}%-18s${RESET} %s\n" "$name" "http://localhost:${port}"
done
LAN_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || true)
printf "  ${BOLD}%-18s${RESET} %s\n" "customer-app" "http://localhost:5173"
if [ -n "$LAN_IP" ]; then
  printf "  ${DIM}%-18s${RESET} ${DIM}%s${RESET}\n" "" "http://${LAN_IP}:5173  (same Wi-Fi, e.g. from a phone)"
fi
echo
printf "  ${BOLD}%-18s${RESET} %s\n" "postgres" "localhost:5432  (user/pass: ${POSTGRES_USER}/${POSTGRES_PASSWORD})"
printf "  ${BOLD}%-18s${RESET} %s\n" "mongo" "localhost:27017"
printf "  ${BOLD}%-18s${RESET} %s\n" "redis" "localhost:6379"
printf "  ${BOLD}%-18s${RESET} %s\n" "redis-commander" "http://localhost:9191"
printf "  ${BOLD}%-18s${RESET} %s\n" "redpanda" "localhost:9092"
printf "  ${BOLD}%-18s${RESET} %s\n" "redpanda-console" "http://localhost:9093"
echo
printf "  ${DIM}Logs:  ./logs/*.log${RESET}\n"
printf "  ${DIM}Stop:  ./stop-infra.sh${RESET}\n\n"
