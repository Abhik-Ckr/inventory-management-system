package com.inventory.system.service.impl;

import com.inventory.system.dto.SaleItemRequest;
import com.inventory.system.dto.SaleItemResponse;
import com.inventory.system.dto.SaleRequest;
import com.inventory.system.dto.SaleResponse;
import com.inventory.system.exception.InsufficientStockException;
import com.inventory.system.exception.ProductNotFoundException;
import com.inventory.system.exception.SaleNotFoundException;
import com.inventory.system.model.*;
import com.inventory.system.repository.InventoryRepository;
import com.inventory.system.repository.ProductRepository;
import com.inventory.system.repository.SaleRepository;
import com.inventory.system.repository.StockMovementRepository;
import com.inventory.system.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional
    public SaleResponse createSale(SaleRequest request) {
        // Save the header first so we have a Sale ID to reference on the
        // stock movements written for this transaction.
        Sale sale = saleRepository.save(Sale.builder().status(SaleStatus.PENDING).build());

        BigDecimal total = BigDecimal.ZERO;

        for (SaleItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ProductNotFoundException(itemRequest.productId()));
            Inventory inventory = inventoryRepository.findByProductId(product.getId())
                    .orElseThrow(() -> new ProductNotFoundException(
                            "No inventory record for product id: " + product.getId()));

            if (inventory.getQuantityOnHand() < itemRequest.quantity()) {
                throw new InsufficientStockException(product.getName(), inventory.getQuantityOnHand(), itemRequest.quantity());
            }

            BigDecimal discount = itemRequest.discount() != null ? itemRequest.discount() : BigDecimal.ZERO;
            BigDecimal subtotal = product.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.quantity()))
                    .subtract(discount);

            SaleItem saleItem = SaleItem.builder()
                    .product(product)
                    .quantity(itemRequest.quantity())
                    .unitPrice(product.getUnitPrice())
                    .discount(discount)
                    .subtotal(subtotal)
                    .build();
            sale.addItem(saleItem);
            total = total.add(subtotal);

            inventory.setQuantityOnHand(inventory.getQuantityOnHand() - itemRequest.quantity());
            inventoryRepository.save(inventory);

            stockMovementRepository.save(StockMovement.builder()
                    .inventory(inventory)
                    .type(MovementType.OUT)
                    .quantity(itemRequest.quantity())
                    .reason("Sale")
                    .reference("SALE-" + sale.getId())
                    .build());
        }

        sale.setTotalAmount(total);
        sale.setStatus(SaleStatus.COMPLETED);
        sale = saleRepository.save(sale);

        return toResponse(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponse getSale(Long id) {
        Sale sale = saleRepository.findById(id).orElseThrow(() -> new SaleNotFoundException(id));
        return toResponse(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleResponse> getAllSales() {
        return saleRepository.findAll().stream().map(this::toResponse).toList();
    }

    private SaleResponse toResponse(Sale sale) {
        List<SaleItemResponse> items = sale.getItems().stream()
                .map(item -> new SaleItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getDiscount(),
                        item.getSubtotal()))
                .toList();

        return new SaleResponse(
                sale.getId(),
                sale.getSaleDate(),
                sale.getTotalAmount(),
                sale.getStatus().name(),
                items);
    }
}
