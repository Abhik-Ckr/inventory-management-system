package com.inventory.system.controller;

import com.inventory.system.dto.AvailabilityResponse;
import com.inventory.system.dto.ProductResponse;
import com.inventory.system.dto.StockAdjustmentRequest;
import com.inventory.system.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}/availability")
    public AvailabilityResponse checkAvailability(
            @PathVariable Long productId,
            @RequestParam @Min(value = 1, message = "Quantity must be positive") int quantity) {
        return inventoryService.checkAvailability(productId, quantity);
    }

    @PatchMapping("/{productId}/add")
    public ProductResponse addStock(@PathVariable Long productId, @Valid @RequestBody StockAdjustmentRequest request) {
        return inventoryService.addStock(productId, request);
    }

    @PatchMapping("/{productId}/remove")
    public ProductResponse removeStock(@PathVariable Long productId, @Valid @RequestBody StockAdjustmentRequest request) {
        return inventoryService.removeStock(productId, request);
    }
}
