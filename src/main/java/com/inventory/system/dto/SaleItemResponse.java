package com.inventory.system.dto;

import java.math.BigDecimal;

public record SaleItemResponse(
        Long productId,
        String sku,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        BigDecimal subtotal
) {
}
