#!/usr/bin/env bash
#
# Loads environment variables from .env and starts the Spring Boot app.
# Usage: ./run.sh
#
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -f .env ]]; then
  echo "ERROR: .env not found. Create one from the template:" >&2
  echo "  cp .env.example .env   # then edit it with your DB credentials" >&2
  exit 1
fi

# Load .env: export every non-comment, non-blank KEY=VALUE line.
set -a
# shellcheck disable=SC1091
source .env
set +a

if [[ -z "${DB_PASSWORD:-}" ]]; then
  echo "ERROR: DB_PASSWORD is not set in .env — the app cannot start without it." >&2
  exit 1
fi

echo "Starting inventory-system (DB user: ${DB_USERNAME:-postgres})..."
exec ./gradlew bootRun
