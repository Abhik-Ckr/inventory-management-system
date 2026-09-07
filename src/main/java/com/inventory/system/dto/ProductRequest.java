package com.inventory.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Payload for creating a new product.")
public record ProductRequest(
        @Schema(description = "Unique stock-keeping unit. Must not already exist.", example = "KB-1001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "SKU is required") String sku,

        @Schema(description = "Product display name.", example = "Mechanical Keyboard", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Name is required") String name,

        @Schema(description = "Optional longer description.", example = "87-key tenkeyless mechanical keyboard, brown switches")
        String description,

        @Schema(description = "Optional category label.", example = "Peripherals")
        String category,

        @Schema(description = "Selling price per unit. Cannot be negative.", example = "79.99", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", message = "Unit price cannot be negative") BigDecimal unitPrice,

        @Schema(description = "Initial quantity in stock. Cannot be negative.", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Initial quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative") Integer quantityOnHand,

        @Schema(description = "Quantity at or below which the product is flagged low-stock.", example = "10")
        @Min(value = 0, message = "Reorder level cannot be negative") Integer reorderLevel,

        @Schema(description = "Optional maximum stock the product may hold (e.g. warehouse cap).", example = "500")
        Integer maxStockLevel
) {
}
