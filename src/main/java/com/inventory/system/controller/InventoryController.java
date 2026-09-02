package com.inventory.system.controller;

import com.inventory.system.dto.ProductResponse;
import com.inventory.system.dto.StockAdjustmentRequest;
import com.inventory.system.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PatchMapping("/{productId}/add")
    public ProductResponse addStock(@PathVariable Long productId, @Valid @RequestBody StockAdjustmentRequest request) {
        return inventoryService.addStock(productId, request);
    }

    @PatchMapping("/{productId}/remove")
    public ProductResponse removeStock(@PathVariable Long productId, @Valid @RequestBody StockAdjustmentRequest request) {
        return inventoryService.removeStock(productId, request);
    }
}
