#!/usr/bin/env bash
#
# End-to-end demo of all three core requirements against a running instance.
#   1. Start the service:  mvn spring-boot:run
#   2. Run this script:     ./examples.sh
#
# Requires: curl, and (optionally) jq for pretty output. Falls back to raw output without jq.
set -euo pipefail

BASE="${BASE:-http://localhost:8086}"
pretty() { if command -v jq >/dev/null 2>&1; then jq .; else cat; fi; }
hr() { printf '\n=== %s ===\n' "$1"; }

hr "1. PROPORTIONAL DISTRIBUTION — create an order paid 50%% wallet / 30%% card / 20%% bank (USD→PEN @ 3.75)"
ORDER=$(curl -s -X POST "$BASE/api/orders" -H 'Content-Type: application/json' -d '{
  "displayCurrency": "USD",
  "processingCurrency": "PEN",
  "exchangeRate": 3.75,
  "payments": [
    { "method": "WALLET",        "amount": 500.00 },
    { "method": "CREDIT_CARD",   "amount": 300.00 },
    { "method": "BANK_TRANSFER", "amount": 200.00 }
  ]
}')
echo "$ORDER" | pretty
ORDER_ID=$(echo "$ORDER" | (command -v jq >/dev/null 2>&1 && jq -r .id || sed -E 's/.*"id":"([^"]+)".*/\1/'))
echo "Created order: $ORDER_ID"

hr "Refund \$300 — expect 150 / 90 / 60 in USD, and 1125 / 675 / 450 reflected in PEN"
curl -s -X POST "$BASE/api/orders/$ORDER_ID/refunds" -H 'Content-Type: application/json' -d '{
  "amount": 300.00, "reasonCode": "PARTIAL_CANCELLATION"
}' | pretty

hr "2. MULTI-CURRENCY — refund the awkward EUR→USD even three-way seed order (ord_1014)"
echo "Refund 100.00 EUR across three equal methods — cents must total exactly, never 99 or 101"
curl -s -X POST "$BASE/api/orders/ord_1014/refunds" -H 'Content-Type: application/json' -d '{
  "amount": 100.00, "reasonCode": "ITEM_OUT_OF_STOCK"
}' | pretty

hr "3. AUDIT TRAIL — issue three separate partial refunds on ord_1013, then read the history"
for amt in 100.00 250.50 75.25; do
  curl -s -X POST "$BASE/api/orders/ord_1013/refunds" -H 'Content-Type: application/json' \
    -d "{ \"amount\": $amt, \"reasonCode\": \"CUSTOMER_REQUEST\" }" > /dev/null
  echo "  issued refund of $amt"
done
echo "Full refund history for ord_1013:"
curl -s "$BASE/api/orders/ord_1013/refunds" | pretty

hr "ERROR HANDLING — refund more than remains (expect 422 EXCEEDS_REFUNDABLE)"
curl -s -X POST "$BASE/api/orders/ord_1004/refunds" -H 'Content-Type: application/json' -d '{
  "amount": 999999.00, "reasonCode": "OTHER"
}' | pretty

hr "ERROR HANDLING — unknown order (expect 404 ORDER_NOT_FOUND)"
curl -s "$BASE/api/orders/ord_nope/refunds" | pretty
