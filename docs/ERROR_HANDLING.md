# Error Handling Reference

This document catalogs every error scenario the Inventory System handles, the
HTTP status it maps to, and the exact user input that triggers it. All errors
are produced centrally by `GlobalExceptionHandler` (`@RestControllerAdvice`) and
returned in a single, consistent response shape.

## Response shape

Every error returns an `ErrorResponse` body:

```json
{
  "timestamp": "2026-09-03T10:15:30.123Z",
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

- `fieldErrors` is present **only** for request-body validation failures
  (`MethodArgumentNotValidException`); it is `null`/absent otherwise.
- All other handlers populate `message` with a human-readable description.

## Status code conventions

| Status | Meaning in this system |
|--------|------------------------|
| **400 Bad Request** | Malformed input — bean-validation failures, duplicate sale line items, invalid query/path params. |
| **404 Not Found** | A referenced product or sale does not exist. |
| **409 Conflict** | Duplicate SKU, or a concurrent update (optimistic lock). |
| **422 Unprocessable Entity** | Well-formed input that violates a business rule (stock/lifecycle constraints). |
| **500 Internal Server Error** | Genuinely unexpected server-side failure (internals hidden from the client). |

---

## Errors by endpoint

### `POST /api/products` — Create product

Body: `ProductRequest`

| Invalid input | Status | Message |
|---------------|--------|---------|
| `sku` blank or missing | 400 | SKU is required |
| `name` blank or missing | 400 | Name is required |
| `unitPrice` missing | 400 | Unit price is required |
| `unitPrice` negative | 400 | Unit price cannot be negative |
| `quantityOnHand` missing | 400 | Initial quantity is required |
| `quantityOnHand` negative | 400 | Quantity cannot be negative |
| `reorderLevel` negative | 400 | Reorder level cannot be negative |
| `sku` already exists | 409 | A product with SKU '...' already exists |

### `GET /api/products/{id}` — Get product by id

| Invalid input | Status | Message |
|---------------|--------|---------|
| `id` does not exist | 404 | Product not found with id: ... |

### `GET /api/products/search?q=` — Search products

| Invalid input | Status | Message |
|---------------|--------|---------|
| `q` blank, whitespace-only, or missing | 400 | Search term is required |

### `PUT /api/products/{id}` — Update product

Body: `ProductUpdateRequest` (all fields optional; only non-null fields applied)

| Invalid input | Status | Message |
|---------------|--------|---------|
| `id` does not exist | 404 | Product not found with id: ... |
| `unitPrice` negative | 400 | Unit price cannot be negative |
| Concurrent update of the same row | 409 | This record was modified by another request. Please retry. |

### `DELETE /api/products/{id}` — Deactivate product

| Invalid input | Status | Message |
|---------------|--------|---------|
| `id` does not exist | 404 | Product not found with id: ... |

### `POST /api/sales` — Create sale

Body: `SaleRequest` → list of `SaleItemRequest`

| Invalid input | Status | Message |
|---------------|--------|---------|
| `items` empty or missing | 400 | A sale needs at least one item |
| item `productId` missing | 400 | Product ID is required |
| item `quantity` missing | 400 | Quantity is required |
| item `quantity` < 1 | 400 | Quantity must be positive |
| item `discount` negative | 400 | Discount cannot be negative |
| Same `productId` on more than one line | 400 | Product id ... appears more than once in the sale; combine it into a single line item |
| `productId` does not exist | 404 | Product not found with id: ... |
| Product is inactive/discontinued | 422 | Product '...' (SKU ...) is inactive and cannot be sold |
| Requested quantity > stock on hand | 422 | Insufficient stock for '...': available X, requested Y |

### `GET /api/sales/{id}` — Get sale by id

| Invalid input | Status | Message |
|---------------|--------|---------|
| `id` does not exist | 404 | Sale not found with id: ... |

### `GET /api/inventory/{productId}/availability?quantity=` — Check availability

| Invalid input | Status | Message |
|---------------|--------|---------|
| `quantity` < 1 | 400 | Quantity must be positive |
| `productId` does not exist | 404 | Product not found with id: ... |

### `PATCH /api/inventory/{productId}/add` — Add stock

Body: `StockAdjustmentRequest`

| Invalid input | Status | Message |
|---------------|--------|---------|
| `quantity` missing | 400 | Quantity is required |
| `quantity` < 1 | 400 | Quantity must be positive |
| `productId` does not exist | 404 | Product not found with id: ... |
| Would exceed the product's `maxStockLevel` | 422 | Adding N unit(s) to '...' would exceed the maximum stock level: current X, max Y |

### `PATCH /api/inventory/{productId}/remove` — Remove stock

Body: `StockAdjustmentRequest`

| Invalid input | Status | Message |
|---------------|--------|---------|
| `quantity` missing | 400 | Quantity is required |
| `quantity` < 1 | 400 | Quantity must be positive |
| `productId` does not exist | 404 | Product not found with id: ... |
| More than current stock on hand | 422 | Insufficient stock for '...': available X, requested Y |

### `GET /api/reports/sales?from=&to=` — Sales report

| Invalid input | Status | Message |
|---------------|--------|---------|
| `from` or `to` missing | 400 | (missing required request parameter) |
| `from` or `to` not an ISO date (e.g. `2026-13-40`) | 400 | (date parse failure) |

---

## Exception → handler reference

| Exception | Status | Handler method |
|-----------|--------|----------------|
| `ProductNotFoundException` | 404 | `handleNotFound` |
| `SaleNotFoundException` | 404 | `handleNotFound` |
| `DuplicateSkuException` | 409 | `handleConflict` |
| `OptimisticLockingFailureException` | 409 | `handleConflict` |
| `InsufficientStockException` | 422 | `handleBusinessRule` |
| `InactiveProductException` | 422 | `handleBusinessRule` |
| `StockLimitExceededException` | 422 | `handleBusinessRule` |
| `DuplicateSaleItemException` | 400 | `handleBadRequest` |
| `ConstraintViolationException` | 400 | `handleBadRequest` |
| `MethodArgumentNotValidException` | 400 | `handleValidation` (adds `fieldErrors`) |
| `Exception` (catch-all) | 500 | `handleUnexpected` |

---

## Known gaps

These user-triggerable inputs currently fall through to the generic **500**
handler instead of returning a more precise **400**:

- **Malformed or empty JSON body** on any `POST`/`PUT`/`PATCH`
  (`HttpMessageNotReadableException`).
- **Non-numeric path or query variable** where a number is expected — e.g.
  `GET /api/products/abc` or `?quantity=xyz`
  (`MethodArgumentTypeMismatchException`).
- **Database constraint violation** other than the SKU uniqueness check
  (`DataIntegrityViolationException`).

Adding dedicated handlers for these would return a clear 400 (or 409 for the
last one) instead of a 500.
