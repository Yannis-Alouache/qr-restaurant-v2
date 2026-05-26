#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker/docker-compose.yml"
POSTGRES_VOLUME="docker_postgres_data"
APP_ENV_VALUE="${APP_ENV:-}"

load_env_file() {
  local env_file=""

  if [[ -f "$ROOT_DIR/.env" ]]; then
    env_file="$ROOT_DIR/.env"
  elif [[ -f "$ROOT_DIR/.env.example" ]]; then
    env_file="$ROOT_DIR/.env.example"
  fi

  if [[ -n "$env_file" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$env_file"
    set +a
  fi
}

ensure_local_environment() {
  if [[ "${CI:-}" == "true" || "${GITHUB_ACTIONS:-}" == "true" ]]; then
    echo "reset-db is disabled outside local development environments." >&2
    exit 1
  fi

  if [[ "$APP_ENV_VALUE" != "local" ]]; then
    echo "reset-db is disabled. Set APP_ENV=local in your local .env to allow it." >&2
    exit 1
  fi
}

confirm_reset() {
  local answer=""

  if [[ ! -t 0 ]]; then
    echo "reset-db requires an interactive terminal confirmation." >&2
    exit 1
  fi

  printf '\n'
  printf '========================================\n'
  printf ' ATTENTION\n'
  printf '========================================\n'
  printf 'Cette action va vider la base de donnee locale.\n'
  printf 'Continuer ? (oui/non): '
  read -r answer

  if [[ "$answer" != "oui" ]]; then
    echo "Reset cancelled." >&2
    exit 1
  fi
}

resolve_compose_command() {
  if docker compose version >/dev/null 2>&1; then
    echo "docker compose"
    return
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    echo "docker-compose"
    return
  fi

  echo "Neither 'docker compose' nor 'docker-compose' is available." >&2
  exit 1
}

load_env_file
APP_ENV_VALUE="${APP_ENV:-}"
ensure_local_environment
confirm_reset

COMPOSE_COMMAND="$(resolve_compose_command)"

$COMPOSE_COMMAND -f "$COMPOSE_FILE" down

if docker volume inspect "$POSTGRES_VOLUME" >/dev/null 2>&1; then
  docker volume rm "$POSTGRES_VOLUME"
else
  echo "Volume $POSTGRES_VOLUME not found, skipping removal." >&2
fi

$COMPOSE_COMMAND -f "$COMPOSE_FILE" up -d
