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
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for the reporting endpoints against H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerIntegrationTest {

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

    private Long lowStockProductId;

    @BeforeEach
    void seed() {
        stockMovementRepository.deleteAll();
        saleRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();

        // Healthy stock.
        Product ok = productRepository.save(Product.builder()
                .sku("SKU-OK").name("Healthy").unitPrice(BigDecimal.valueOf(10)).active(true).build());
        inventoryRepository.save(Inventory.builder()
                .product(ok).quantityOnHand(50).reorderLevel(10).build());

        // At/below reorder level -> low stock.
        Product low = productRepository.save(Product.builder()
                .sku("SKU-LOW").name("Running Low").unitPrice(BigDecimal.valueOf(20)).active(true).build());
        inventoryRepository.save(Inventory.builder()
                .product(low).quantityOnHand(3).reorderLevel(10).build());
        lowStockProductId = low.getId();
    }

    @Test
    void stockReport_listsAllActiveProducts() throws Exception {
        mockMvc.perform(get("/api/reports/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    void lowStockReport_listsOnlyProductsAtOrBelowReorder() throws Exception {
        mockMvc.perform(get("/api/reports/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(lowStockProductId))
                .andExpect(jsonPath("$[0].lowStock").value(true));
    }

    @Test
    void salesReport_aggregatesCompletedSalesInRange() throws Exception {
        // Record a sale today so it falls inside the report window.
        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(lowStockProductId, 2, null)));
        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Widen the window by a day on each side so the sale's UTC timestamp
        // falls inside it regardless of the machine's local time zone.
        LocalDate today = LocalDate.now();
        mockMvc.perform(get("/api/reports/sales")
                        .param("from", today.minusDays(1).toString())
                        .param("to", today.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].totalQuantitySold").value(2))
                .andExpect(jsonPath("$.totalRevenue").value(40));
    }

    @Test
    void salesReport_returnsEmpty_whenFromAfterTo() throws Exception {
        // The service does not reject an inverted range; the JPQL BETWEEN
        // simply matches nothing, so the report comes back empty with 200.
        mockMvc.perform(get("/api/reports/sales")
                        .param("from", "2026-09-30")
                        .param("to", "2026-09-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(jsonPath("$.totalRevenue").value(0));
    }

    @Test
    void salesReport_returnsEmpty_whenNoSalesInRange() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .param("from", "2000-01-01")
                        .param("to", "2000-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(jsonPath("$.totalRevenue").value(0));
    }
}
