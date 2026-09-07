package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.dto.SaleItemRequest;
import com.inventory.system.dto.SaleRequest;
import com.inventory.system.model.Inventory;
import com.inventory.system.model.Product;
import com.inventory.system.repository.InventoryRepository;
import com.inventory.system.repository.ProductRepository;
import com.inventory.system.repository.SaleRepository;
import com.inventory.system.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for the sales endpoints against H2. These
 * verify that recording a sale computes totals and decrements real stock.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SaleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    private Long productId;

    @BeforeEach
    void seed() {
        stockMovementRepository.deleteAll();
        saleRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();

        Product product = productRepository.save(Product.builder()
                .sku("SKU-SALE").name("Widget").unitPrice(BigDecimal.valueOf(10)).active(true).build());
        inventoryRepository.save(Inventory.builder()
                .product(product).quantityOnHand(50).reorderLevel(5).build());
        productId = product.getId();
    }

    private Long seedInactiveProduct() {
        Product product = productRepository.save(Product.builder()
                .sku("SKU-OFF").name("Discontinued").unitPrice(BigDecimal.valueOf(10)).active(false).build());
        inventoryRepository.save(Inventory.builder()
                .product(product).quantityOnHand(50).reorderLevel(0).build());
        return product.getId();
    }

    private Long seedProduct(String sku, BigDecimal price, int qty) {
        Product product = productRepository.save(Product.builder()
                .sku(sku).name(sku).unitPrice(price).active(true).build());
        inventoryRepository.save(Inventory.builder()
                .product(product).quantityOnHand(qty).reorderLevel(0).build());
        return product.getId();
    }

    @Test
    void create_recordsSaleWithMultipleProducts() throws Exception {
        // Second product at a different price; the seeded one (SKU-SALE) is $10.
        Long secondId = seedProduct("SKU-SALE2", BigDecimal.valueOf(20), 30);
        SaleRequest request = new SaleRequest(List.of(
                new SaleItemRequest(productId, 2, null),   // 2 * 10 = 20
                new SaleItemRequest(secondId, 3, null)));  // 3 * 20 = 60

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.totalAmount").value(80));

        assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getQuantityOnHand())
                .isEqualTo(48);
        assertThat(inventoryRepository.findByProductId(secondId).orElseThrow().getQuantityOnHand())
                .isEqualTo(27);
    }

    @Test
    void create_rollsBackAllStock_whenOneLineItemFails() throws Exception {
        // First line succeeds in isolation, second line has too little stock.
        // The whole sale must roll back: no stock moved, no sale persisted.
        Long shortId = seedProduct("SKU-SHORT", BigDecimal.valueOf(10), 1);
        SaleRequest request = new SaleRequest(List.of(
                new SaleItemRequest(productId, 5, null),
                new SaleItemRequest(shortId, 100, null)));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());

        // The first item's decrement must have been rolled back.
        assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getQuantityOnHand())
                .isEqualTo(50);
        assertThat(inventoryRepository.findByProductId(shortId).orElseThrow().getQuantityOnHand())
                .isEqualTo(1);
        // And no sale header should have survived the rollback.
        assertThat(saleRepository.count()).isZero();
    }

    @Test
    void create_recordsSaleAndDecrementsStock() throws Exception {
        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(productId, 5, null)));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalAmount").value(50))
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)));

        assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getQuantityOnHand())
                .isEqualTo(45);
    }

    @Test
    void create_appliesDiscountToSubtotal() throws Exception {
        SaleRequest request = new SaleRequest(List.of(
                new SaleItemRequest(productId, 5, BigDecimal.valueOf(10))));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(40));
    }

    @Test
    void create_returns422_whenInsufficientStock() throws Exception {
        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(productId, 1000, null)));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        // Stock must be untouched when the sale is rejected.
        assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getQuantityOnHand())
                .isEqualTo(50);
    }

    @Test
    void create_returns422_whenProductInactive() throws Exception {
        Long inactiveId = seedInactiveProduct();
        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(inactiveId, 1, null)));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void create_returns404_whenProductUnknown() throws Exception {
        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(9999L, 1, null)));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returns400_whenDuplicateLineItems() throws Exception {
        SaleRequest request = new SaleRequest(List.of(
                new SaleItemRequest(productId, 1, null),
                new SaleItemRequest(productId, 2, null)));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenNoItems() throws Exception {
        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_returnsRecordedSale() throws Exception {
        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(productId, 2, null)));
        String body = mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        long saleId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/sales/{id}", saleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saleId));
    }

    @Test
    void getById_returns404_whenUnknown() throws Exception {
        mockMvc.perform(get("/api/sales/{id}", 9999))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_returnsRecordedSales() throws Exception {
        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(productId, 1, null)));
        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }
}
