package com.inventory.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "A single line item in a recorded sale.")
public record SaleItemResponse(
        @Schema(description = "Product id.", example = "1") Long productId,
        @Schema(description = "Product SKU.", example = "KB-1001") String sku,
        @Schema(description = "Product name.", example = "Mechanical Keyboard") String productName,
        @Schema(description = "Units sold.", example = "2") int quantity,
        @Schema(description = "Unit price at time of sale.", example = "79.99") BigDecimal unitPrice,
        @Schema(description = "Discount applied to this line.", example = "5.00") BigDecimal discount,
        @Schema(description = "Line subtotal: quantity * unitPrice - discount.", example = "154.98") BigDecimal subtotal
) {
}
