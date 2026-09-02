package com.inventory.system.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        String category,
        BigDecimal unitPrice,
        boolean active,
        int quantityOnHand,
        int reorderLevel,
        Integer maxStockLevel,
        boolean lowStock,
        Instant createdAt,
        Instant updatedAt
) {
}
