package com.inventory.system.controller;

import com.inventory.system.dto.ErrorResponse;
import com.inventory.system.dto.SalesReportResponse;
import com.inventory.system.dto.StockReportItem;
import com.inventory.system.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Stock and sales reporting.")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Stock report", description = "Current stock level for every product.")
    @ApiResponse(responseCode = "200", description = "Stock levels for all products")
    @GetMapping("/stock")
    public List<StockReportItem> stockReport() {
        return reportService.getStockReport();
    }

    @Operation(summary = "Low-stock report",
            description = "Products whose quantity on hand is at or below their reorder level.")
    @ApiResponse(responseCode = "200", description = "Products that need reordering")
    @GetMapping("/low-stock")
    public List<StockReportItem> lowStockReport() {
        return reportService.getLowStockReport();
    }

    @Operation(summary = "Sales report",
            description = "Per-product sales totals and overall revenue between two dates (inclusive). Dates are ISO format, yyyy-MM-dd.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales figures for the requested range"),
            @ApiResponse(responseCode = "400", description = "A date parameter is missing or not a valid ISO date",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/sales")
    public SalesReportResponse salesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.getSalesReport(from, to);
    }
}
