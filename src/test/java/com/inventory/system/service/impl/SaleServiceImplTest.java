package com.inventory.system.service.impl;

import com.inventory.system.dto.SaleItemRequest;
import com.inventory.system.dto.SaleRequest;
import com.inventory.system.dto.SaleResponse;
import com.inventory.system.exception.InsufficientStockException;
import com.inventory.system.model.Inventory;
import com.inventory.system.model.Product;
import com.inventory.system.model.Sale;
import com.inventory.system.model.SaleStatus;
import com.inventory.system.repository.InventoryRepository;
import com.inventory.system.repository.ProductRepository;
import com.inventory.system.repository.SaleRepository;
import com.inventory.system.repository.StockMovementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceImplTest {

    @Mock
    private SaleRepository saleRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private SaleServiceImpl saleService;

    @Test
    void createSale_reducesStockAndComputesTotal_whenStockIsSufficient() {
        Product product = Product.builder().id(1L).sku("SKU-1").name("Widget")
                .unitPrice(BigDecimal.valueOf(10)).active(true).build();
        Inventory inventory = Inventory.builder().id(1L).product(product)
                .quantityOnHand(50).reorderLevel(5).build();

        when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            if (s.getId() == null) s.setId(100L);
            return s;
        });
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(1L, 5, null)));

        SaleResponse response = saleService.createSale(request);

        assertThat(response.status()).isEqualTo(SaleStatus.COMPLETED.name());
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(inventory.getQuantityOnHand()).isEqualTo(45);
    }

    @Test
    void createSale_throwsInsufficientStockException_whenNotEnoughStock() {
        Product product = Product.builder().id(1L).sku("SKU-1").name("Widget")
                .unitPrice(BigDecimal.TEN).active(true).build();
        Inventory inventory = Inventory.builder().id(1L).product(product)
                .quantityOnHand(2).reorderLevel(0).build();

        when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            if (s.getId() == null) s.setId(101L);
            return s;
        });
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(1L, 10, null)));

        assertThatThrownBy(() -> saleService.createSale(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Widget");
    }
}
