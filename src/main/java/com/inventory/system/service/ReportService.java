package com.inventory.system.service;

import com.inventory.system.dto.SalesReportResponse;
import com.inventory.system.dto.StockReportItem;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    List<StockReportItem> getStockReport();
    List<StockReportItem> getLowStockReport();
    SalesReportResponse getSalesReport(LocalDate from, LocalDate to);
}
