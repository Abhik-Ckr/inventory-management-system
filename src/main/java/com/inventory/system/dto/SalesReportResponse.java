package com.inventory.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Sales report over a date range, with per-product breakdown and total revenue.")
public record SalesReportResponse(
        @Schema(description = "Start of the reporting range (inclusive).", example = "2026-09-01") LocalDate from,
        @Schema(description = "End of the reporting range (inclusive).", example = "2026-09-30") LocalDate to,
        @Schema(description = "Per-product sales figures.") List<SalesReportItem> items,
        @Schema(description = "Total revenue across all products in the range.", example = "8123.45") BigDecimal totalRevenue
) {
}
