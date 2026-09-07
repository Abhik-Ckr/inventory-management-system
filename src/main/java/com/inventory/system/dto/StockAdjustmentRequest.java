package com.inventory.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for adding or removing stock for a product.")
public record StockAdjustmentRequest(
        @Schema(description = "Number of units to add or remove. Must be positive.", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be positive") Integer quantity,

        @Schema(description = "Optional reason recorded with the stock movement.", example = "Supplier delivery #4821")
        String reason
) {
}
