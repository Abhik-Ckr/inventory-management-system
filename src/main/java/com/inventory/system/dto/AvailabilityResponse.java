package com.inventory.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Answer to a "can I get N of this product?" check. Returned with 200 when
 * the requested quantity can be fulfilled — {@code status} carries a
 * machine-readable verdict and {@code suggestion} a next action for the UI.
 * When stock is short the endpoint throws instead of returning this, so a
 * successful body always means available.
 */
@Schema(description = "Result of a stock-availability check. A 200 body always means the request can be fulfilled.")
public record AvailabilityResponse(
        @Schema(description = "Product id.", example = "1") Long productId,
        @Schema(description = "Product SKU.", example = "KB-1001") String sku,
        @Schema(description = "Product name.", example = "Mechanical Keyboard") String productName,
        @Schema(description = "Quantity that was requested.", example = "10") int requestedQuantity,
        @Schema(description = "Current quantity in stock.", example = "50") int quantityOnHand,
        @Schema(description = "Stock left if the request were fulfilled.", example = "40") int remainingIfFulfilled,
        @Schema(description = "Whether the request can be fulfilled.", example = "true") boolean available,
        @Schema(description = "Machine-readable verdict.", example = "AVAILABLE") String status,
        @Schema(description = "Human-readable message.", example = "10 unit(s) available for Mechanical Keyboard") String message,
        @Schema(description = "Suggested next action for the UI.", example = "PROCEED") String suggestion
) {
}
