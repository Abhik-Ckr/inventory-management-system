package com.inventory.system.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        Long id,
        Instant saleDate,
        BigDecimal totalAmount,
        String status,
        List<SaleItemResponse> items
) {
}
