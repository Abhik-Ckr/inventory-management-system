package com.inventory.system.dto;

public record StockReportItem(
        Long productId,
        String sku,
        String name,
        int quantityOnHand,
        int reorderLevel,
        boolean lowStock
) {
}
