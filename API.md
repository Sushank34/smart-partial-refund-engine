# API Reference

Base URL: `http://localhost:8086` · Content type: `application/json` · Amounts in major units.

Interactive docs: `/swagger-ui.html`.

---

## POST /api/orders

Record an order. The order total is derived from the sum of `payments`.

**Request**

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `displayCurrency` | string | yes | ISO code shown to the customer, e.g. `USD` |
| `processingCurrency` | string | yes | Currency the payment was processed in, e.g. `PEN` |
| `exchangeRate` | number | conditional | display → processing rate. Optional if currencies match (defaults to 1); required otherwise |
| `payments` | array (1–4) | yes | Each `{ method, amount }` |
| `payments[].method` | enum | yes | `WALLET` \| `CREDIT_CARD` \| `DEBIT_CARD` \| `BANK_TRANSFER` |
| `payments[].amount` | number | yes | > 0, in display currency |

**201 Created** → [OrderResponse](#orderresponse).

```bash
curl -X POST http://localhost:8086/api/orders -H 'Content-Type: application/json' -d '{
  "displayCurrency":"USD","processingCurrency":"PEN","exchangeRate":3.75,
  "payments":[
    {"method":"WALLET","amount":500.00},
    {"method":"CREDIT_CARD","amount":300.00},
    {"method":"BANK_TRANSFER","amount":200.00}
  ]}'
```

---

## GET /api/orders

List all orders — useful for discovering the seeded demo data.

**200 OK** → array of [OrderResponse](#orderresponse).

```bash
curl http://localhost:8086/api/orders
```

---

## GET /api/orders/{orderId}

**200 OK** → [OrderResponse](#orderresponse). **404** `ORDER_NOT_FOUND` if unknown.

```bash
curl http://localhost:8086/api/orders/ord_1011
```

---

## POST /api/orders/{orderId}/refunds

Issue a partial (or full) refund. Computes the proportional, multi-currency breakdown,
enforces the remaining-balance ceiling, and persists the audit record.

**Request**

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `amount` | number | yes | > 0, in the order's display currency |
| `reasonCode` | enum | yes | `PARTIAL_CANCELLATION` \| `ITEM_OUT_OF_STOCK` \| `DAMAGED_GOODS` \| `CUSTOMER_REQUEST` \| `OTHER` |

**201 Created** → [RefundResponse](#refundresponse).

```bash
curl -X POST http://localhost:8086/api/orders/ord_1011/refunds \
  -H 'Content-Type: application/json' \
  -d '{"amount":300.00,"reasonCode":"PARTIAL_CANCELLATION"}'
```

---

## GET /api/orders/{orderId}/refunds

Full refund history, oldest first.

**200 OK** → array of [RefundResponse](#refundresponse). **404** `ORDER_NOT_FOUND` if unknown.

```bash
curl http://localhost:8086/api/orders/ord_1011/refunds
```

---

## Schemas

### OrderResponse

```json
{
  "id": "ord_1011",
  "displayCurrency": "USD",
  "processingCurrency": "PEN",
  "exchangeRate": 3.75,
  "totalAmount": 1000.00,
  "totalRefunded": 450.00,
  "refundableRemaining": 550.00,
  "status": "ACTIVE",
  "payments": [ { "method": "WALLET", "amount": 500.00 } ],
  "createdAt": "2026-06-08T09:28:04.657153Z"
}
```

`status` ∈ `ACTIVE` | `FULLY_REFUNDED`.

### RefundResponse

```json
{
  "id": "rf_b7477d36",
  "orderId": "ord_1011",
  "reasonCode": "PARTIAL_CANCELLATION",
  "status": "COMPLETED",
  "note": null,
  "displayCurrency": "USD",
  "processingCurrency": "PEN",
  "exchangeRate": 3.75,
  "requestedDisplayAmount": 300.00,
  "requestedProcessingAmount": 1125.00,
  "allocations": [
    { "method": "WALLET", "displayAmount": 150.00, "processingAmount": 562.50, "instant": true, "settlementDays": 0 }
  ],
  "createdAt": "2026-06-08T09:28:05.244236Z"
}
```

`status` ∈ `COMPLETED` | `FLAGGED` (with `note` explaining the constraint breach).

### ErrorResponse

```json
{ "status": 422, "code": "EXCEEDS_REFUNDABLE", "error": "Refund of 999999.00 exceeds the remaining refundable balance of 75.00." }
```

| code | HTTP | Meaning |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | Field constraint failed (e.g. non-positive amount) |
| `MALFORMED_REQUEST` | 400 | Unparseable body or invalid enum value |
| `ORDER_NOT_FOUND` | 404 | No order with that id |
| `CONCURRENT_MODIFICATION` | 409 | Another refund modified the order concurrently; retry |
| `EXCEEDS_REFUNDABLE` | 422 | Refund would exceed the remaining balance |
| `EXCHANGE_RATE_REQUIRED` | 422 | Rate omitted when currencies differ |
| `INVALID_EXCHANGE_RATE` | 422 | Rate ≤ 0 |
