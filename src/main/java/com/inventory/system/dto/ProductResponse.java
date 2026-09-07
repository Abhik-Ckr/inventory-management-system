package com.inventory.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "A product with its current inventory state.")
public record ProductResponse(
        @Schema(description = "Product id.", example = "1") Long id,
        @Schema(description = "Stock-keeping unit.", example = "KB-1001") String sku,
        @Schema(description = "Product name.", example = "Mechanical Keyboard") String name,
        @Schema(description = "Description.", example = "87-key tenkeyless mechanical keyboard, brown switches") String description,
        @Schema(description = "Category.", example = "Peripherals") String category,
        @Schema(description = "Unit price.", example = "79.99") BigDecimal unitPrice,
        @Schema(description = "Whether the product is active (sellable).", example = "true") boolean active,
        @Schema(description = "Current quantity in stock.", example = "50") int quantityOnHand,
        @Schema(description = "Low-stock threshold.", example = "10") int reorderLevel,
        @Schema(description = "Maximum stock level, if configured.", example = "500") Integer maxStockLevel,
        @Schema(description = "True when quantityOnHand is at or below reorderLevel.", example = "false") boolean lowStock,
        @Schema(description = "Creation timestamp.", example = "2026-09-01T09:30:00Z") Instant createdAt,
        @Schema(description = "Last update timestamp.", example = "2026-09-05T14:12:00Z") Instant updatedAt
) {
}
