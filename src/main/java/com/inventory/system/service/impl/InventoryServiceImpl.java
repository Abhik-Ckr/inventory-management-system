package com.inventory.system.service.impl;

import com.inventory.system.dto.ProductResponse;
import com.inventory.system.dto.StockAdjustmentRequest;
import com.inventory.system.exception.InsufficientStockException;
import com.inventory.system.exception.ProductNotFoundException;
import com.inventory.system.model.Inventory;
import com.inventory.system.model.MovementType;
import com.inventory.system.model.Product;
import com.inventory.system.model.StockMovement;
import com.inventory.system.repository.InventoryRepository;
import com.inventory.system.repository.ProductRepository;
import com.inventory.system.repository.StockMovementRepository;
import com.inventory.system.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional
    public ProductResponse addStock(Long productId, StockAdjustmentRequest request) {
        Product product = findProductOrThrow(productId);
        Inventory inventory = findInventoryOrThrow(productId);

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + request.quantity());
        inventory = inventoryRepository.save(inventory);

        stockMovementRepository.save(StockMovement.builder()
                .inventory(inventory)
                .type(MovementType.IN)
                .quantity(request.quantity())
                .reason(request.reason())
                .build());

        return toResponse(product, inventory);
    }

    @Override
    @Transactional
    public ProductResponse removeStock(Long productId, StockAdjustmentRequest request) {
        Product product = findProductOrThrow(productId);
        Inventory inventory = findInventoryOrThrow(productId);

        if (inventory.getQuantityOnHand() < request.quantity()) {
            throw new InsufficientStockException(product.getName(), inventory.getQuantityOnHand(), request.quantity());
        }

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() - request.quantity());
        inventory = inventoryRepository.save(inventory);

        stockMovementRepository.save(StockMovement.builder()
                .inventory(inventory)
                .type(MovementType.ADJUSTMENT)
                .quantity(request.quantity())
                .reason(request.reason())
                .build());

        return toResponse(product, inventory);
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Inventory findInventoryOrThrow(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException("No inventory record for product id: " + productId));
    }

    private ProductResponse toResponse(Product product, Inventory inventory) {
        return new ProductResponse(
                product.getId(), product.getSku(), product.getName(), product.getDescription(),
                product.getCategory(), product.getUnitPrice(), product.isActive(),
                inventory.getQuantityOnHand(), inventory.getReorderLevel(), inventory.getMaxStockLevel(),
                inventory.isLowStock(), product.getCreatedAt(), product.getUpdatedAt());
    }
}
