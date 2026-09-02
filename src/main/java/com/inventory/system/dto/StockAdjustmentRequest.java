package com.inventory.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be positive") Integer quantity,
        String reason
) {
}
