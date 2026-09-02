package com.inventory.system.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * Every field is optional — only non-null fields are applied. This lets a
 * client update just the price without needing to resend the whole product.
 */
public record ProductUpdateRequest(
        String name,
        String description,
        String category,
        @DecimalMin(value = "0.0", message = "Unit price cannot be negative") BigDecimal unitPrice,
        Boolean active,
        Integer reorderLevel,
        Integer maxStockLevel
) {
}
