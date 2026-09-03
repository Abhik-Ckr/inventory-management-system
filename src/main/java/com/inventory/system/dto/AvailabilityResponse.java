package com.inventory.system.dto;

/**
 * Answer to a "can I get N of this product?" check. Returned with 200 when
 * the requested quantity can be fulfilled — {@code status} carries a
 * machine-readable verdict and {@code suggestion} a next action for the UI.
 * When stock is short the endpoint throws instead of returning this, so a
 * successful body always means available.
 */
public record AvailabilityResponse(
        Long productId,
        String sku,
        String productName,
        int requestedQuantity,
        int quantityOnHand,
        int remainingIfFulfilled,
        boolean available,
        String status,
        String message,
        String suggestion
) {
}
