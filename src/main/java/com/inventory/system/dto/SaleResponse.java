package com.inventory.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "A recorded sale with its line items and total.")
public record SaleResponse(
        @Schema(description = "Sale id.", example = "1001") Long id,
        @Schema(description = "When the sale was recorded.", example = "2026-09-05T14:20:00Z") Instant saleDate,
        @Schema(description = "Total amount across all line items.", example = "154.98") BigDecimal totalAmount,
        @Schema(description = "Sale status.", example = "COMPLETED") String status,
        @Schema(description = "Line items in the sale.") List<SaleItemResponse> items
) {
}
