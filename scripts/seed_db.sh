#!/usr/bin/env bash
# seed_db.sh — Wipe user/shipment-related data and repopulate with demo
# shipper/traveler accounts + shipments across every lifecycle stage.
#
# The actual seed data lives in DataSeeder.java (runs on backend startup),
# so this script just clears the relevant tables and (re)starts the backend
# to trigger it — that way passwords are hashed through the app's real
# BCryptPasswordEncoder instead of being hand-rolled here.
#
# Usage:
#   ./scripts/seed_db.sh

set -eo pipefail

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BACKEND_DIR"

DB_NAME="logistics_app"
DB_USER="logistics_user"
DB_PASSWORD="7812"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

log()  { echo -e "${CYAN}[seed]${RESET} $*"; }
ok()   { echo -e "${GREEN}[seed]${RESET} $*"; }
warn() { echo -e "${YELLOW}[seed]${RESET} $*"; }
err()  { echo -e "${RED}[seed]${RESET} $*"; }

echo ""
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${CYAN}   🌱  Flyship — Wipe & Reseed Demo Data${RESET}"
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""

# ── Wipe data (children before parents to satisfy FK constraints) ──────────
log "Clearing users/shipments and dependent tables (cities are kept)..."
mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" <<'SQL'
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM payments;
DELETE FROM wallet_transactions;
DELETE FROM wallet_locks;
DELETE FROM shipment_histories;
DELETE FROM quotes;
DELETE FROM travel_plans;
DELETE FROM wallets;
DELETE FROM shipments;
DELETE FROM users;
ALTER TABLE payments AUTO_INCREMENT = 1;
ALTER TABLE wallet_transactions AUTO_INCREMENT = 1;
ALTER TABLE wallet_locks AUTO_INCREMENT = 1;
ALTER TABLE shipment_histories AUTO_INCREMENT = 1;
ALTER TABLE quotes AUTO_INCREMENT = 1;
ALTER TABLE travel_plans AUTO_INCREMENT = 1;
ALTER TABLE wallets AUTO_INCREMENT = 1;
ALTER TABLE shipments AUTO_INCREMENT = 1;
ALTER TABLE users AUTO_INCREMENT = 1;
SET FOREIGN_KEY_CHECKS = 1;
SQL
ok "Tables cleared."

# ── Restart backend so DataSeeder (CommandLineRunner) repopulates data ─────
PID_FILE="$BACKEND_DIR/server.pid"
LOG_FILE="$BACKEND_DIR/server.log"

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  log "Stopping running backend (PID $(cat "$PID_FILE"))..."
  kill "$(cat "$PID_FILE")"
  rm -f "$PID_FILE"
  sleep 2
fi

log "Starting backend to trigger seeding (this compiles + boots Spring Boot, ~30-60s)..."
: > "$LOG_FILE"
mvn -q spring-boot:run > "$LOG_FILE" 2>&1 &
SERVER_PID=$!
echo $SERVER_PID > "$PID_FILE"

log "Waiting for seeding to complete..."
SEEDED=0
for i in $(seq 1 90); do
  if grep -q "Demo shipments seeded" "$LOG_FILE" 2>/dev/null; then
    SEEDED=1
    break
  fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    err "Backend process exited early. Check server.log for errors."
    exit 1
  fi
  sleep 1
done

if [[ "$SEEDED" -ne 1 ]]; then
  err "Timed out waiting for seeding to finish. Check server.log."
  exit 1
fi

ok "Backend seeded successfully (PID $SERVER_PID, logs: server.log)."

# ── Summary ──────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}${GREEN}Demo accounts (password: 123456):${RESET}"
echo "  shipper1@flyship.test .. shipper5@flyship.test"
echo "  traveler1@flyship.test .. traveler5@flyship.test"
echo ""
echo -e "${BOLD}${GREEN}Demo shipments seeded:${RESET}"
echo "  2x submitted        (pending, no quotes)"
echo "  2x quotation given   (pending, quote pending)"
echo "  2x quotation accepted (accepted, escrow funded)"
echo "  2x shipping completed (delivered, full history trail)"
echo ""
ok "Done."
