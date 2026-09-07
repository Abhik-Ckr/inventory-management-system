package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.dto.ProductRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for the product endpoints: controller,
 * service, and JPA repositories against an in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

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

    @BeforeEach
    void cleanDatabase() {
        stockMovementRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
    }

    private Product seedProduct(String sku, String name, int qty, int reorder, boolean active) {
        Product product = productRepository.save(Product.builder()
                .sku(sku).name(name).unitPrice(BigDecimal.valueOf(10)).active(active).build());
        inventoryRepository.save(Inventory.builder()
                .product(product).quantityOnHand(qty).reorderLevel(reorder).build());
        return product;
    }

    @Test
    void create_persistsProductAndInventory() throws Exception {
        ProductRequest request = new ProductRequest("SKU-100", "Keyboard", "Nice", "Peripherals",
                BigDecimal.valueOf(79.99), 50, 10, 500);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sku").value("SKU-100"))
                .andExpect(jsonPath("$.quantityOnHand").value(50))
                .andExpect(jsonPath("$.active").value(true));

        org.assertj.core.api.Assertions.assertThat(productRepository.existsBySku("SKU-100")).isTrue();
    }

    @Test
    void create_returns409_whenSkuAlreadyExists() throws Exception {
        seedProduct("SKU-DUP", "Existing", 5, 0, true);
        ProductRequest request = new ProductRequest("SKU-DUP", "Another", null, null,
                BigDecimal.TEN, 1, 0, null);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getById_returnsSeededProduct() throws Exception {
        Product product = seedProduct("SKU-1", "Widget", 20, 5, true);

        mockMvc.perform(get("/api/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-1"))
                .andExpect(jsonPath("$.quantityOnHand").value(20));
    }

    @Test
    void getById_returns404_whenUnknown() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 9999))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_returnsOnlyActiveProducts() throws Exception {
        seedProduct("SKU-A", "Active", 10, 0, true);
        seedProduct("SKU-B", "Inactive", 10, 0, false);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].sku").value("SKU-A"));
    }

    @Test
    void search_findsActiveByPartialName() throws Exception {
        seedProduct("KB-1", "Mechanical Keyboard", 10, 0, true);
        seedProduct("MS-1", "Mouse", 10, 0, true);

        mockMvc.perform(get("/api/products/search").param("q", "keyb"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].sku").value("KB-1"));
    }

    @Test
    void search_returns400_whenTermBlank() throws Exception {
        mockMvc.perform(get("/api/products/search").param("q", "  "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_appliesPartialChanges() throws Exception {
        Product product = seedProduct("SKU-U", "Old Name", 10, 5, true);

        mockMvc.perform(put("/api/products/{id}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\",\"unitPrice\":25.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.unitPrice").value(25.50));
    }

    @Test
    void deactivate_softDeletesProduct() throws Exception {
        Product product = seedProduct("SKU-D", "ToRemove", 10, 0, true);

        mockMvc.perform(delete("/api/products/{id}", product.getId()))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(
                productRepository.findById(product.getId()).orElseThrow().isActive()).isFalse();
    }
}
