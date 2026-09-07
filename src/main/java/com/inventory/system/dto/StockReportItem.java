package com.inventory.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stock level for a single product in a stock report.")
public record StockReportItem(
        @Schema(description = "Product id.", example = "1") Long productId,
        @Schema(description = "Product SKU.", example = "KB-1001") String sku,
        @Schema(description = "Product name.", example = "Mechanical Keyboard") String name,
        @Schema(description = "Current quantity in stock.", example = "8") int quantityOnHand,
        @Schema(description = "Low-stock threshold.", example = "10") int reorderLevel,
        @Schema(description = "True when at or below the reorder level.", example = "true") boolean lowStock
) {
}
