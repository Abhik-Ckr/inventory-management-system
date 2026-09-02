package com.inventory.system.dto;

import java.math.BigDecimal;

public record SalesReportItem(
        Long productId,
        String sku,
        String name,
        long totalQuantitySold,
        BigDecimal totalRevenue
) {
}
