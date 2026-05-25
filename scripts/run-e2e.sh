#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
API_LOG="$(mktemp)"
CLIENT_LOG="$(mktemp)"
ADMIN_LOG="$(mktemp)"

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

API_PID="$(start_if_missing \
  "http://localhost:8080/api/public/menu/naia-burger" \
  "api" \
  "$API_LOG" \
  env STRIPE_SECRET_KEY=sk_test_dummy STRIPE_PUBLIC_KEY=pk_test_dummy STRIPE_WEBHOOK_SECRET=whsec_test mvn -q spring-boot:run)"

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
