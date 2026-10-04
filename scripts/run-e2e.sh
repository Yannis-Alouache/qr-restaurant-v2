#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
API_LOG="$(mktemp)"
CLIENT_LOG="$(mktemp)"
ADMIN_LOG="$(mktemp)"

# Les specs écrivent (commandes, comptes, emails) : elles doivent aller dans une
# base dédiée, jamais dans qr_restaurant. La surcharge OS env l'emporte sur le
# .env lu par l'API (spring.config.import).
E2E_DB_NAME="qr_restaurant_e2e"
E2E_DB_URL="jdbc:postgresql://localhost:5432/${E2E_DB_NAME}"
DB_CONTAINER="qr-restaurant-db"
DB_USER="$(grep -E '^POSTGRES_USER=' "$ROOT_DIR/.env" 2>/dev/null | cut -d= -f2 | tr -d '[:space:]' || true)"
DB_USER="${DB_USER:-qr_user}"

# Un serveur déjà présent sur le 8080 serait réutilisé tel quel : s'il pointe
# vers la base de dev, les specs écriraient dedans. On refuse plutôt. Le test
# par socket couvre aussi les non-API (phpMyAdmin, proxys Docker…) qui bloquent
# le port sans répondre 200 sur /actuator/health.
ensure_port_free() {
  local occupied=""
  if command -v netstat >/dev/null 2>&1; then
    occupied="$(netstat -ano 2>/dev/null | grep -E '[:.]8080[[:space:]]' | grep -i listen | head -1 || true)"
  elif curl -sf "http://localhost:8080/actuator/health" >/dev/null 2>&1; then
    occupied="/actuator/health répond"
  fi

  if [[ -n "$occupied" ]]; then
    echo "Le port 8080 est déjà occupé : ${occupied}" >&2
    echo "Le e2e démarre sa propre API sur ce port et sur la base dédiée ${E2E_DB_NAME}." >&2
    echo "Arrête ce qui occupe le 8080 (API de dev, phpMyAdmin, proxy…), puis relance." >&2
    exit 1
  fi
}

ensure_e2e_database() {
  if ! docker ps --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
    echo "PostgreSQL n'est pas démarré :" >&2
    echo "  docker compose -f docker/docker-compose.yml up -d postgres" >&2
    exit 1
  fi

  # « up -d » rend la main avant le healthcheck : le conteneur peut exister
  # sans que postgres accepte déjà des connexions (race vue en CI).
  for _ in $(seq 1 30); do
    if docker exec "$DB_CONTAINER" pg_isready -U "$DB_USER" -q; then
      break
    fi
    sleep 1
  done

  if ! docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d postgres -tAc \
      "SELECT 1 FROM pg_database WHERE datname = '${E2E_DB_NAME}'" | grep -q 1; then
    echo "Création de la base e2e dédiée : ${E2E_DB_NAME}" >&2
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE ${E2E_DB_NAME}"
  fi
}

API_PID=""
CLIENT_PID=""
ADMIN_PID=""

cleanup() {
  for pid in "$API_PID" "$CLIENT_PID" "$ADMIN_PID"; do
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid"
      wait "$pid" 2>/dev/null || true
    fi
  done
}

trap cleanup EXIT

wait_for_url() {
  local url="$1"
  local label="$2"

  for _ in $(seq 1 180); do
    if curl -sf "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  echo "Timed out while waiting for ${label}: ${url}" >&2
  echo "--- API log ---" >&2
  cat "$API_LOG" >&2 || true
  echo "--- Client log ---" >&2
  cat "$CLIENT_LOG" >&2 || true
  echo "--- Admin log ---" >&2
  cat "$ADMIN_LOG" >&2 || true
  exit 1
}

start_if_missing() {
  local url="$1"
  local label="$2"
  local log_file="$3"
  shift 3

  if curl -sf "$url" >/dev/null 2>&1; then
    echo "Reusing existing ${label} at ${url}" >&2
    return 0
  fi

  (
    cd "$ROOT_DIR/$label"
    "$@"
  ) >"$log_file" 2>&1 &

  echo $!
}

ensure_e2e_database
ensure_port_free

# Maven wrapper committé dans api/ : pas de prérequis d'installation Maven.
# mvnw.cmd est un batch Windows — réservé à Git Bash, sinon on prend mvnw.
if [[ "$(uname -s)" =~ ^(MINGW|MSYS|CYGWIN) && -f "$ROOT_DIR/api/mvnw.cmd" ]]; then
  MVN_CMD="./mvnw.cmd"
else
  MVN_CMD="./mvnw"
fi

API_PID="$(start_if_missing \
  "http://localhost:8080/api/public/menu/naia-burger" \
  "api" \
  "$API_LOG" \
  env STRIPE_SECRET_KEY=sk_test_dummy STRIPE_PUBLIC_KEY=pk_test_dummy STRIPE_WEBHOOK_SECRET=whsec_test SEED_DEMO_DATA=true DATABASE_URL="$E2E_DB_URL" "$MVN_CMD" -q spring-boot:run)"

CLIENT_PID="$(start_if_missing \
  "http://localhost:4300/menu/naia-burger/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01" \
  "client" \
  "$CLIENT_LOG" \
  npm start -- --host localhost --port 4300)"

ADMIN_PID="$(start_if_missing \
  "http://localhost:4200/login" \
  "admin" \
  "$ADMIN_LOG" \
  npm start -- --host localhost --port 4200)"

wait_for_url "http://localhost:8080/api/public/menu/naia-burger" "API"
wait_for_url "http://localhost:4300/menu/naia-burger/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01" "client"
wait_for_url "http://localhost:4200/login" "admin"

cd "$ROOT_DIR"
npx playwright test "$@"
