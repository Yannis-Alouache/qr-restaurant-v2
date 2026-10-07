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

# Clés Stripe : le parcours « vrai checkout » (client-payment-journey.spec.ts)
# exige une clé secrète de test réelle pour créer une session Checkout hébergée.
# On lit le .env (même source que l'API) mais on n'accepte qu'une clé de test —
# jamais de sk_live_*. Sans clé exploitable, l'API tourne avec des clés factices
# et le spec s'auto-désactive (E2E_STRIPE_REAL=false) au lieu de faire échouer
# toute la suite.
stripe_secret="$(grep -E '^STRIPE_SECRET_KEY=' "$ROOT_DIR/.env" 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '[:space:]' || true)"
stripe_public="$(grep -E '^STRIPE_PUBLIC_KEY=' "$ROOT_DIR/.env" 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '[:space:]' || true)"
e2e_stripe_real=false
if [[ "$stripe_secret" == sk_test_* ]]; then
  e2e_stripe_real=true
else
  stripe_secret="sk_test_dummy"
  stripe_public="pk_test_dummy"
fi
case "$stripe_public" in
  pk_test_*) ;;
  *) stripe_public="pk_test_dummy" ;;
esac

export E2E_STRIPE_REAL="$e2e_stripe_real"

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
  #
  # On sonde en TCP (-h localhost), jamais sur la socket Unix : au premier
  # démarrage (volume frais = chaque run CI), l'entrypoint de l'image lance
  # un serveur temporaire pour initdb, à l'écoute de la seule socket, puis
  # l'arrête. Un pg_isready socket réussit donc pendant cette fenêtre, et la
  # CREATE DATABASE qui suit meurt sur « the database system is shutting
  # down » ou « terminating connection due to administrator command »
  # (~1 run sur 2 en CI depuis le 04/10/2026). Le serveur temporaire n'écoute
  # pas TCP : -h localhost ne sursature que le serveur réel.
  db_ready=0
  for _ in $(seq 1 30); do
    if docker exec "$DB_CONTAINER" pg_isready -h localhost -U "$DB_USER" -q; then
      db_ready=1
      break
    fi
    sleep 1
  done
  if [[ "$db_ready" -ne 1 ]]; then
    echo "PostgreSQL n'accepte pas de connexion TCP après 30 s sur ${DB_CONTAINER}." >&2
    docker logs --tail 50 "$DB_CONTAINER" >&2 || true
    exit 1
  fi

  # Existence + création en un seul aller-retour (\gexec, via stdin car -c
  # n'interprète pas les méta-commandes) : idempotent et sans fenêtre
  # TOCTOU entre le test et le CREATE DATABASE.
  docker exec -i "$DB_CONTAINER" psql -h localhost -U "$DB_USER" -d postgres -q <<SQL
SELECT 'CREATE DATABASE ${E2E_DB_NAME}' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${E2E_DB_NAME}') \gexec
SQL
  echo "Base e2e dédiée prête : ${E2E_DB_NAME}" >&2
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
  env STRIPE_SECRET_KEY="$stripe_secret" STRIPE_PUBLIC_KEY="$stripe_public" STRIPE_WEBHOOK_SECRET=whsec_test SEED_DEMO_DATA=true DATABASE_URL="$E2E_DB_URL" "$MVN_CMD" -q spring-boot:run)"

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
