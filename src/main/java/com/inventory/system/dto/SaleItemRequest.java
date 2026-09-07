package com.inventory.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "A single line item within a sale.")
public record SaleItemRequest(
        @Schema(description = "Id of the product being sold.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Product ID is required") Long productId,

        @Schema(description = "Units sold. Must be positive.", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be positive") Integer quantity,

        @Schema(description = "Optional per-line discount amount. Cannot be negative.", example = "5.00")
        @DecimalMin(value = "0.0", message = "Discount cannot be negative") BigDecimal discount
) {
}
