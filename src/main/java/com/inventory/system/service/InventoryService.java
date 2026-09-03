package com.inventory.system.service;

import com.inventory.system.dto.AvailabilityResponse;
import com.inventory.system.dto.ProductResponse;
import com.inventory.system.dto.StockAdjustmentRequest;

public interface InventoryService {
    ProductResponse addStock(Long productId, StockAdjustmentRequest request);
    ProductResponse removeStock(Long productId, StockAdjustmentRequest request);

    /**
     * Checks whether {@code quantity} units of the product can be fulfilled
     * right now. Returns an availability verdict when there is enough stock;
     * throws InsufficientStockException (422) when there isn't, or
     * InactiveProductException (422) if the product has been discontinued.
     */
    AvailabilityResponse checkAvailability(Long productId, int quantity);
}
