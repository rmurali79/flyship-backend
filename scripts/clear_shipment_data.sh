#!/usr/bin/env bash
# clear_shipment_data.sh — Wipe itinerary, shipment, and quotation data
# (and everything that hangs off of it: quote payments, shipment status
# history, shipment chat messages, and any shipment/quote wallet locks).
#
# Users, wallets, wallet transaction history, and cities are left untouched.
#
# Usage:
#   ./scripts/clear_shipment_data.sh

set -eo pipefail

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BACKEND_DIR"

DB_NAME="logistics_app"
DB_USER="logistics_user"
DB_PASSWORD="7812"

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

log() { echo -e "${CYAN}[clear]${RESET} $*"; }
ok()  { echo -e "${GREEN}[clear]${RESET} $*"; }
err() { echo -e "${RED}[clear]${RESET} $*"; }

echo ""
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${CYAN}   🧹  Flyship — Clear Itinerary / Shipment / Quotation Data${RESET}"
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""

log "Clearing shipments, quotes, travel plans, and dependent tables (users/wallets kept)..."
mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" <<'SQL'
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM payments;
DELETE FROM messages;
DELETE FROM shipment_histories;
DELETE FROM wallet_locks WHERE type IN ('shipment_budget', 'quote_collateral');
DELETE FROM quotes;
DELETE FROM shipments;
DELETE FROM travel_plans;
ALTER TABLE payments AUTO_INCREMENT = 1;
ALTER TABLE messages AUTO_INCREMENT = 1;
ALTER TABLE shipment_histories AUTO_INCREMENT = 1;
ALTER TABLE quotes AUTO_INCREMENT = 1;
ALTER TABLE shipments AUTO_INCREMENT = 1;
ALTER TABLE travel_plans AUTO_INCREMENT = 1;
SET FOREIGN_KEY_CHECKS = 1;
SQL

ok "Itinerary, shipment, and quotation data cleared."
