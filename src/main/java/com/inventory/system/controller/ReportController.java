package com.inventory.system.controller;

import com.inventory.system.dto.SalesReportResponse;
import com.inventory.system.dto.StockReportItem;
import com.inventory.system.service.ReportService;
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
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/stock")
    public List<StockReportItem> stockReport() {
        return reportService.getStockReport();
    }

    @GetMapping("/low-stock")
    public List<StockReportItem> lowStockReport() {
        return reportService.getLowStockReport();
    }

    @GetMapping("/sales")
    public SalesReportResponse salesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.getSalesReport(from, to);
    }
}
