package com.inventory.system.service.impl;

import com.inventory.system.dto.ProductRequest;
import com.inventory.system.dto.ProductResponse;
import com.inventory.system.exception.DuplicateSkuException;
import com.inventory.system.model.Inventory;
import com.inventory.system.model.Product;
import com.inventory.system.repository.InventoryRepository;
import com.inventory.system.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createProduct_savesProductAndInventory_whenSkuIsUnique() {
        ProductRequest request = new ProductRequest(
                "SKU-001", "Widget", "A widget", "Hardware",
                BigDecimal.valueOf(9.99), 100, 10, 500);

        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> {
            Inventory i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        ProductResponse response = productService.createProduct(request);

        assertThat(response.sku()).isEqualTo("SKU-001");
        assertThat(response.quantityOnHand()).isEqualTo(100);
        assertThat(response.lowStock()).isFalse();
    }

    @Test
    void createProduct_throwsDuplicateSkuException_whenSkuAlreadyExists() {
        ProductRequest request = new ProductRequest(
                "SKU-001", "Widget", null, null,
                BigDecimal.TEN, 10, 0, null);

        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining("SKU-001");
    }
}
