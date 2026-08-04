#!/usr/bin/env bash
# seed_city_images.sh — Point each city's image_url at the locally-bundled
# photo in src/main/resources/static/city-images/ (served at /city-images/*
# by Spring Boot's default static resource handling), instead of the
# external Unsplash/Wikimedia hotlinks that shipped with the original seed
# data (several of which had gone dead: Berlin, Chennai, Hong Kong).
#
# Cities with no reliable local photo are set to NULL — the frontend and
# mobile apps fall back to a code tile (e.g. "BER") for those, and for any
# image that fails to load at runtime.
#
# Safe to re-run any time (idempotent).
#
# Usage:
#   ./scripts/seed_city_images.sh

set -eo pipefail

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BACKEND_DIR"

DB_NAME="logistics_app"
DB_USER="logistics_user"
DB_PASSWORD="7812"

GREEN='\033[0;32m'
CYAN='\033[0;36m'
RESET='\033[0m'

echo -e "${CYAN}[cities]${RESET} Updating city image_url values..."

mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" <<'SQL'
UPDATE cities SET image_url = CONCAT('/city-images/', LOWER(code), '.jpg')
WHERE code IN ('DXB','LHR','LAX','BOM','NYC','CDG','SIN','SYD','HND');

UPDATE cities SET image_url = NULL WHERE code IN ('BER','CHE','HKG');
SQL

echo -e "${GREEN}[cities]${RESET} Done. Cities without a local photo (Berlin, Chennai, Hong Kong) will show their code tile in the app."
