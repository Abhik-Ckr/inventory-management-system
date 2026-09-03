# Inventory System — API Reference

Base URL: `http://localhost:8080`
Content type: `application/json` for every request and response body.

All timestamps are ISO-8601 (`Instant`, UTC). Money fields are decimals with two
places. Errors share a single JSON envelope — see [Error handling](#error-handling).

## Contents
- [Products](#products)
- [Inventory](#inventory)
- [Sales](#sales)
- [Reports](#reports)
- [Error handling](#error-handling)

---

## Products

### Create a product
`POST /api/products`

Creates a catalog entry and its paired inventory record in one call.

**Request body**
```json
{
  "sku": "WIDGET-001",
  "name": "Blue Widget",
  "description": "Standard blue widget",
  "category": "Widgets",
  "unitPrice": 19.99,
  "quantityOnHand": 100,
  "reorderLevel": 10,
  "maxStockLevel": 500
}
```

| Field | Required | Rule |
|---|---|---|
| `sku` | yes | non-blank, unique |
| `name` | yes | non-blank |
| `description` | no | |
| `category` | no | |
| `unitPrice` | yes | ≥ 0 |
| `quantityOnHand` | yes | ≥ 0 |
| `reorderLevel` | no | ≥ 0 (defaults to 0) |
| `maxStockLevel` | no | ≥ 0 |

**Response — `201 Created`**
```json
{
  "id": 1,
  "sku": "WIDGET-001",
  "name": "Blue Widget",
  "description": "Standard blue widget",
  "category": "Widgets",
  "unitPrice": 19.99,
  "active": true,
  "quantityOnHand": 100,
  "reorderLevel": 10,
  "maxStockLevel": 500,
  "lowStock": false,
  "createdAt": "2026-09-02T10:15:30Z",
  "updatedAt": "2026-09-02T10:15:30Z"
}
```

**Errors:** `409 Conflict` if the SKU already exists · `400 Bad Request` on validation failure.

---

### List products
`GET /api/products`

Returns all **active** products.

**Response — `200 OK`**
```json
[
  {
    "id": 1,
    "sku": "WIDGET-001",
    "name": "Blue Widget",
    "description": "Standard blue widget",
    "category": "Widgets",
    "unitPrice": 19.99,
    "active": true,
    "quantityOnHand": 100,
    "reorderLevel": 10,
    "maxStockLevel": 500,
    "lowStock": false,
    "createdAt": "2026-09-02T10:15:30Z",
    "updatedAt": "2026-09-02T10:15:30Z"
  }
]
```

---

### Search products
`GET /api/products/search?q=keyboard`

Case-insensitive partial match over **name and SKU**, active products only. This is
how a client turns a human search term into product **ids** — because names aren't
unique (you can have two "Keyboard" products), the caller inspects the results and
picks the right one before using its `id` for availability checks or sales.

**Query parameters**

| Param | Required | Notes |
|---|---|---|
| `q` | yes | matched against name and SKU, case-insensitive; blank returns `[]` |

**Response — `200 OK`**
```json
[
  {
    "id": 2,
    "sku": "WIDGET-002",
    "name": "Keyboard",
    "category": "Widgets",
    "unitPrice": 2000.00,
    "active": true,
    "quantityOnHand": 50,
    "reorderLevel": 1,
    "lowStock": false
  },
  {
    "id": 3,
    "sku": "KB-100",
    "name": "Keyboard",
    "category": "Peripherals",
    "unitPrice": 49.99,
    "active": true,
    "quantityOnHand": 2,
    "reorderLevel": 1,
    "lowStock": false
  }
]
```

Returns an empty array `[]` when nothing matches (still `200 OK`).

---

### Get one product
`GET /api/products/{id}`

**Response — `200 OK`** — same shape as a single object above.

**Errors:** `404 Not Found` if no product has that id.

---

### Update a product
`PUT /api/products/{id}`

Partial update — send only the fields you want to change; omitted/null fields are left as-is.

**Request body**
```json
{
  "unitPrice": 24.50,
  "reorderLevel": 20
}
```

| Field | Rule |
|---|---|
| `name` | optional |
| `description` | optional |
| `category` | optional |
| `unitPrice` | optional, ≥ 0 |
| `active` | optional (true/false) |
| `reorderLevel` | optional |
| `maxStockLevel` | optional |

**Response — `200 OK`** — the full updated `ProductResponse`.

**Errors:** `404 Not Found` · `400 Bad Request` on validation failure.

---

### Deactivate a product
`DELETE /api/products/{id}`

Soft-delete: marks the product `active = false`. It stays in the database but is
excluded from `GET /api/products` and can no longer be sold.

**Response — `204 No Content`** (empty body).

**Errors:** `404 Not Found`.

---

## Inventory

Stock is never edited directly — every change goes through these endpoints, which
also write a stock-movement audit row.

### Add stock
`PATCH /api/inventory/{productId}/add`

**Request body**
```json
{
  "quantity": 50,
  "reason": "Purchase order #PO-2026-014"
}
```

| Field | Required | Rule |
|---|---|---|
| `quantity` | yes | ≥ 1 |
| `reason` | no | free text |

**Response — `200 OK`** — the updated `ProductResponse` (see Products) with the new `quantityOnHand`.

**Errors:** `404 Not Found` (product/inventory missing) · `422 Unprocessable Entity` if the
addition would push `quantityOnHand` above `maxStockLevel`.

---

### Remove stock
`PATCH /api/inventory/{productId}/remove`

Manual downward adjustment (damage, shrinkage, correction) — separate from a sale.

**Request body**
```json
{
  "quantity": 5,
  "reason": "Damaged in transit"
}
```

**Response — `200 OK`** — the updated `ProductResponse`.

**Errors:** `404 Not Found` · `422 Unprocessable Entity` if `quantity` exceeds stock on hand.

---

### Check availability
`GET /api/inventory/{productId}/availability?quantity=2`

A lightweight pre-check — "can I get N of this right now?" — for a UI to call
**before** creating a sale. Returns a verdict with a suggested next action when
stock is sufficient, and the same `422` the sale endpoint raises when it isn't.

**Query parameters**

| Param | Required | Rule |
|---|---|---|
| `quantity` | yes | ≥ 1 |

**Response — `200 OK`** (enough stock)
```json
{
  "productId": 3,
  "sku": "KB-100",
  "productName": "Keyboard",
  "requestedQuantity": 2,
  "quantityOnHand": 2,
  "remainingIfFulfilled": 0,
  "available": true,
  "status": "AVAILABLE_LOW_STOCK",
  "message": "2 unit(s) of 'Keyboard' are available (in stock: 2).",
  "suggestion": "You can proceed, but only 0 would remain — at or below the reorder level of 1. Consider restocking soon."
}
```

`status` is `AVAILABLE` when comfortable, or `AVAILABLE_LOW_STOCK` when fulfilling
the request would drop stock to/below the reorder level — in which case
`suggestion` recommends restocking.

**Response — `422 Unprocessable Entity`** (not enough stock)
```json
{
  "timestamp": "2026-09-03T08:04:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Insufficient stock for 'Keyboard': available 2, requested 3",
  "path": "/api/inventory/3/availability",
  "fieldErrors": null
}
```

**Errors:** `404 Not Found` (unknown product) · `422 Unprocessable Entity` (insufficient
stock, or the product is inactive) · `400 Bad Request` if `quantity` < 1.

---

## Sales

### Create a sale
`POST /api/sales`

Records a sale of one or more line items, decrements stock, and computes the total.
Runs in a single transaction — if any line fails, nothing is committed.

**Request body**
```json
{
  "items": [
    { "productId": 1, "quantity": 3, "discount": 2.00 },
    { "productId": 2, "quantity": 1 }
  ]
}
```

| Field | Required | Rule |
|---|---|---|
| `items` | yes | at least one item |
| `items[].productId` | yes | must exist and be active |
| `items[].quantity` | yes | ≥ 1, must not exceed stock on hand |
| `items[].discount` | no | ≥ 0, subtracted from the line subtotal |

**Response — `201 Created`**
```json
{
  "id": 1001,
  "saleDate": "2026-09-02T11:42:07Z",
  "totalAmount": 77.97,
  "status": "COMPLETED",
  "items": [
    {
      "productId": 1,
      "sku": "WIDGET-001",
      "productName": "Blue Widget",
      "quantity": 3,
      "unitPrice": 19.99,
      "discount": 2.00,
      "subtotal": 57.97
    },
    {
      "productId": 2,
      "sku": "GADGET-002",
      "productName": "Red Gadget",
      "quantity": 1,
      "unitPrice": 20.00,
      "discount": 0.00,
      "subtotal": 20.00
    }
  ]
}
```

**Errors:**

| Status | When |
|---|---|
| `404 Not Found` | a `productId` doesn't exist |
| `400 Bad Request` | validation failure, or the same product appears on more than one line |
| `422 Unprocessable Entity` | insufficient stock, or a product is inactive/discontinued |

---

### List sales
`GET /api/sales`

**Response — `200 OK`** — an array of `SaleResponse` objects (shape above).

---

### Get one sale
`GET /api/sales/{id}`

**Response — `200 OK`** — a single `SaleResponse`.

**Errors:** `404 Not Found`.

---

## Reports

### Full stock report
`GET /api/reports/stock`

**Response — `200 OK`**
```json
[
  {
    "productId": 1,
    "sku": "WIDGET-001",
    "name": "Blue Widget",
    "quantityOnHand": 97,
    "reorderLevel": 10,
    "lowStock": false
  }
]
```

### Low-stock report
`GET /api/reports/low-stock`

Same shape as the stock report, filtered to items at or below their reorder level.

### Sales report
`GET /api/reports/sales?from=2026-08-01&to=2026-08-31`

**Query parameters**

| Param | Required | Format |
|---|---|---|
| `from` | yes | `YYYY-MM-DD` |
| `to` | yes | `YYYY-MM-DD` |

**Response — `200 OK`**
```json
{
  "from": "2026-08-01",
  "to": "2026-08-31",
  "items": [
    {
      "productId": 1,
      "sku": "WIDGET-001",
      "name": "Blue Widget",
      "totalQuantitySold": 42,
      "totalRevenue": 839.58
    }
  ],
  "totalRevenue": 839.58
}
```

---

## Error handling

Every error returns the same envelope with the matching HTTP status:

```json
{
  "timestamp": "2026-09-02T11:45:12Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Insufficient stock for 'Blue Widget': available 5, requested 8",
  "path": "/api/sales",
  "fieldErrors": null
}
```

`fieldErrors` is populated only on validation failures, mapping each rejected field
to its message:

```json
{
  "timestamp": "2026-09-02T11:46:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/products",
  "fieldErrors": {
    "sku": "SKU is required",
    "unitPrice": "Unit price cannot be negative"
  }
}
```

### Status reference

| Status | Meaning | Example triggers |
|---|---|---|
| `400 Bad Request` | Malformed input | Failed field validation; same product listed twice in one sale |
| `404 Not Found` | Resource doesn't exist | Unknown product id or sale id |
| `409 Conflict` | State conflict | Duplicate SKU; concurrent update to the same record |
| `422 Unprocessable Entity` | Business-rule violation | Insufficient stock; selling an inactive product; exceeding max stock level |
| `500 Internal Server Error` | Unexpected server failure | Uncaught error (generic message, no internals leaked) |
