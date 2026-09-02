package com.inventory.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaleItemRequest(
        @NotNull(message = "Product ID is required") Long productId,
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be positive") Integer quantity,
        @DecimalMin(value = "0.0", message = "Discount cannot be negative") BigDecimal discount
) {
}
