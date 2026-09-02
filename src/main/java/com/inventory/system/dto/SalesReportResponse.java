package com.inventory.system.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesReportResponse(
        LocalDate from,
        LocalDate to,
        List<SalesReportItem> items,
        BigDecimal totalRevenue
) {
}
