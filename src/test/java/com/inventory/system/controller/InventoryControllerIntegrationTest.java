package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.dto.StockAdjustmentRequest;
import com.inventory.system.model.Inventory;
import com.inventory.system.model.Product;
import com.inventory.system.repository.InventoryRepository;
import com.inventory.system.repository.ProductRepository;
import com.inventory.system.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for the inventory endpoints against H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    private Long productId;

    @BeforeEach
    void seed() {
        stockMovementRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();

        Product product = productRepository.save(Product.builder()
                .sku("SKU-INV").name("Widget").unitPrice(BigDecimal.valueOf(10)).active(true).build());
        inventoryRepository.save(Inventory.builder()
                .product(product).quantityOnHand(50).reorderLevel(10).maxStockLevel(100).build());
        productId = product.getId();
    }

    @Test
    void checkAvailability_returns200_whenEnoughStock() throws Exception {
        mockMvc.perform(get("/api/inventory/{id}/availability", productId).param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.remainingIfFulfilled").value(40));
    }

    @Test
    void checkAvailability_returns422_whenNotEnoughStock() throws Exception {
        mockMvc.perform(get("/api/inventory/{id}/availability", productId).param("quantity", "1000"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void checkAvailability_returns400_whenQuantityNotPositive() throws Exception {
        mockMvc.perform(get("/api/inventory/{id}/availability", productId).param("quantity", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addStock_increasesQuantityAndRecordsMovement() throws Exception {
        mockMvc.perform(patch("/api/inventory/{id}/add", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(30, "delivery"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(80));

        assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getQuantityOnHand())
                .isEqualTo(80);
        assertThat(stockMovementRepository.findAll()).hasSize(1);
    }

    @Test
    void addStock_returns422_whenExceedsMaxStockLevel() throws Exception {
        mockMvc.perform(patch("/api/inventory/{id}/add", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(100, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        // Rejected adjustment must not have changed the stored quantity.
        assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getQuantityOnHand())
                .isEqualTo(50);
    }

    @Test
    void removeStock_decreasesQuantity() throws Exception {
        mockMvc.perform(patch("/api/inventory/{id}/remove", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(20, "damage"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(30));
    }

    @Test
    void removeStock_returns422_whenInsufficientStock() throws Exception {
        mockMvc.perform(patch("/api/inventory/{id}/remove", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(1000, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void concurrentInventoryWrite_triggersOptimisticLock() throws Exception {
        // Two callers read the same inventory row (same @Version). This is the
        // race two simultaneous sales hit. Each repository call runs in its own
        // transaction, so these are distinct detached snapshots.
        Inventory first = inventoryRepository.findByProductId(productId).orElseThrow();
        Inventory second = inventoryRepository.findByProductId(productId).orElseThrow();

        // First writer wins and bumps the version.
        first.setQuantityOnHand(first.getQuantityOnHand() - 5);
        inventoryRepository.saveAndFlush(first);

        // Second writer is now stale -> optimistic-lock failure. The global
        // handler turns this into a 409 for the caller.
        second.setQuantityOnHand(second.getQuantityOnHand() - 3);
        assertThatThrownBy(() -> inventoryRepository.saveAndFlush(second))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
    }

    @Test
    void addStock_returns404_whenProductUnknown() throws Exception {
        mockMvc.perform(patch("/api/inventory/{id}/add", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(5, null))))
                .andExpect(status().isNotFound());
    }
}
