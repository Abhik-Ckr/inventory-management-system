package com.inventory.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * Every field is optional — only non-null fields are applied. This lets a
 * client update just the price without needing to resend the whole product.
 */
@Schema(description = "Partial update for a product. Only non-null fields are applied.")
public record ProductUpdateRequest(
        @Schema(description = "New display name.", example = "Mechanical Keyboard (2024)")
        String name,

        @Schema(description = "New description.", example = "Updated hot-swappable model")
        String description,

        @Schema(description = "New category.", example = "Peripherals")
        String category,

        @Schema(description = "New unit price. Cannot be negative.", example = "84.99")
        @DecimalMin(value = "0.0", message = "Unit price cannot be negative") BigDecimal unitPrice,

        @Schema(description = "Activate (true) or deactivate/discontinue (false) the product.", example = "true")
        Boolean active,

        @Schema(description = "New low-stock threshold.", example = "15")
        Integer reorderLevel,

        @Schema(description = "New maximum stock level.", example = "600")
        Integer maxStockLevel
) {
}
