#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# A separate project/port isolates this clean-start check from the demo database.
export COMPOSE_PROJECT_NAME="time-deposit-smoke-${GITHUB_RUN_ID:-$$}"
export DB_PORT=55432
export DB_URL="jdbc:postgresql://localhost:${DB_PORT}/time_deposits"
app_pid=""
cleanup() {
  if [[ -n "$app_pid" ]]; then
    kill "$app_pid" 2>/dev/null || true
    wait "$app_pid" 2>/dev/null || true
  fi
  docker compose down
}
trap cleanup EXIT

docker compose up -d --wait db
start_app() {
  java -jar java/target/time-deposit-kata-1.0-SNAPSHOT.jar >> java/target/smoke-app.log 2>&1 &
  app_pid=$!
  curl --fail --silent --show-error --retry 40 --retry-delay 1 --retry-connrefused \
    http://localhost:8080/time-deposits > java/target/smoke-get.json
}
post_update() {
  status=$(curl --silent --show-error -o java/target/smoke-post.txt -w '%{http_code}' \
    -X POST http://localhost:8080/time-deposits/update-balances)
  [[ "$status" == "204" ]]
  [[ ! -s java/target/smoke-post.txt ]]
}

start_app
python3 - <<'PY'
import json
with open('java/target/smoke-get.json') as f:
    assert json.load(f) == [], 'Fresh database must be empty'
PY

docker compose exec -T db psql -U deposits -d time_deposits -v ON_ERROR_STOP=1 < java/demo/seed.sql
post_update
curl --fail --silent --show-error http://localhost:8080/time-deposits > java/target/smoke-after.json
python3 - <<'PY'
import json
from decimal import Decimal
with open('java/target/smoke-after.json') as f:
    deposits = json.load(f, parse_float=Decimal)
assert [d['balance'] for d in deposits] == [1201, 1203, 1205, 1200, 1200, 1200]
assert [d['days'] for d in deposits] == [31, 365, 46, 366, 45, 60]
assert [len(d['withdrawals']) for d in deposits] == [2, 1, 0, 0, 0, 0]
PY

kill "$app_pid"
wait "$app_pid" || true
app_pid=""
start_app
python3 - <<'PY'
import json
from decimal import Decimal
with open('java/target/smoke-after.json') as f:
    before = json.load(f, parse_float=Decimal)
with open('java/target/smoke-get.json') as f:
    after = json.load(f, parse_float=Decimal)
assert before == after, 'Committed state must survive an application restart'
PY

# Rerunning the demo script must not reset accrued balances.
docker compose exec -T db psql -U deposits -d time_deposits -v ON_ERROR_STOP=1 < java/demo/seed.sql
post_update
curl --fail --silent --show-error http://localhost:8080/time-deposits > java/target/smoke-second.json
python3 - <<'PY'
import json
from decimal import Decimal
with open('java/target/smoke-second.json') as f:
    deposits = json.load(f, parse_float=Decimal)
assert [d['balance'] for d in deposits] == [1202, Decimal('1206.01'), Decimal('1210.02'), 1200, 1200, 1200]
PY
echo 'PASS: packaged application, demo data, two accrual cycles, and restart persistence'
