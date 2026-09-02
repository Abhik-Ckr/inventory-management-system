package com.inventory.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "SKU is required") String sku,
        @NotBlank(message = "Name is required") String name,
        String description,
        String category,
        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", message = "Unit price cannot be negative") BigDecimal unitPrice,
        @NotNull(message = "Initial quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative") Integer quantityOnHand,
        @Min(value = 0, message = "Reorder level cannot be negative") Integer reorderLevel,
        Integer maxStockLevel
) {
}
