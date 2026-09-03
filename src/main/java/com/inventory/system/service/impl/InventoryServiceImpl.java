package com.inventory.system.service.impl;

import com.inventory.system.dto.AvailabilityResponse;
import com.inventory.system.dto.ProductResponse;
import com.inventory.system.dto.StockAdjustmentRequest;
import com.inventory.system.exception.InactiveProductException;
import com.inventory.system.exception.InsufficientStockException;
import com.inventory.system.exception.ProductNotFoundException;
import com.inventory.system.exception.StockLimitExceededException;
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

        // Respect the configured warehouse cap when one is set. A null
        // maxStockLevel means "no ceiling", so we only guard when it exists.
        int newQuantity = inventory.getQuantityOnHand() + request.quantity();
        if (inventory.getMaxStockLevel() != null && newQuantity > inventory.getMaxStockLevel()) {
            throw new StockLimitExceededException(
                    product.getName(), inventory.getQuantityOnHand(), request.quantity(), inventory.getMaxStockLevel());
        }

        inventory.setQuantityOnHand(newQuantity);
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

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(Long productId, int quantity) {
        Product product = findProductOrThrow(productId);

        // A discontinued product can't be sold, so it isn't "available" at any
        // quantity — surface that as the business-rule 422 rather than a
        // misleading "in stock" answer.
        if (!product.isActive()) {
            throw new InactiveProductException(product.getName(), product.getSku());
        }

        Inventory inventory = findInventoryOrThrow(productId);
        int onHand = inventory.getQuantityOnHand();

        // Not enough on hand -> same 422 the sale endpoint raises, so the
        // client gets one consistent "quantity isn't enough" message.
        if (onHand < quantity) {
            throw new InsufficientStockException(product.getName(), onHand, quantity);
        }

        int remaining = onHand - quantity;
        boolean wouldBeLow = remaining <= inventory.getReorderLevel();

        String status = wouldBeLow ? "AVAILABLE_LOW_STOCK" : "AVAILABLE";
        String message = quantity + " unit(s) of '" + product.getName() + "' are available (in stock: " + onHand + ").";
        String suggestion = wouldBeLow
                ? "You can proceed, but only " + remaining + " would remain — at or below the reorder level of "
                        + inventory.getReorderLevel() + ". Consider restocking soon."
                : "You can proceed to create a sale for this quantity.";

        return new AvailabilityResponse(
                product.getId(), product.getSku(), product.getName(),
                quantity, onHand, remaining, true, status, message, suggestion);
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
