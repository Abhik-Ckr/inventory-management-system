package com.inventory.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Aggregated sales figures for a single product.")
public record SalesReportItem(
        @Schema(description = "Product id.", example = "1") Long productId,
        @Schema(description = "Product SKU.", example = "KB-1001") String sku,
        @Schema(description = "Product name.", example = "Mechanical Keyboard") String name,
        @Schema(description = "Total units sold in the period.", example = "37") long totalQuantitySold,
        @Schema(description = "Total revenue from this product in the period.", example = "2921.63") BigDecimal totalRevenue
) {
}
