package com.inventory.system.service;

import com.inventory.system.dto.ProductResponse;
import com.inventory.system.dto.StockAdjustmentRequest;

public interface InventoryService {
    ProductResponse addStock(Long productId, StockAdjustmentRequest request);
    ProductResponse removeStock(Long productId, StockAdjustmentRequest request);
}
