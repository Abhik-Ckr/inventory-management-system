package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.dto.AvailabilityResponse;
import com.inventory.system.dto.ProductResponse;
import com.inventory.system.dto.StockAdjustmentRequest;
import com.inventory.system.exception.InsufficientStockException;
import com.inventory.system.exception.ProductNotFoundException;
import com.inventory.system.exception.StockLimitExceededException;
import com.inventory.system.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer unit tests for {@link InventoryController} with a mocked service.
 */
@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    private ProductResponse productWithStock(int qty) {
        return new ProductResponse(1L, "SKU-001", "Widget", null, null,
                BigDecimal.TEN, true, qty, 10, 500, false, Instant.now(), Instant.now());
    }

    @Test
    void checkAvailability_returns200_whenAvailable() throws Exception {
        AvailabilityResponse response = new AvailabilityResponse(1L, "SKU-001", "Widget",
                10, 50, 40, true, "AVAILABLE", "10 unit(s) available", "PROCEED");
        when(inventoryService.checkAvailability(1L, 10)).thenReturn(response);

        mockMvc.perform(get("/api/inventory/1/availability").param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void checkAvailability_returns400_whenQuantityNotPositive() throws Exception {
        mockMvc.perform(get("/api/inventory/1/availability").param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void checkAvailability_returns404_whenProductMissing() throws Exception {
        when(inventoryService.checkAvailability(99L, 5)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/inventory/99/availability").param("quantity", "5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void checkAvailability_returns422_whenInsufficientStock() throws Exception {
        when(inventoryService.checkAvailability(1L, 100))
                .thenThrow(new InsufficientStockException("Widget", 10, 100));

        mockMvc.perform(get("/api/inventory/1/availability").param("quantity", "100"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void addStock_returns200_whenValid() throws Exception {
        when(inventoryService.addStock(eq(1L), any(StockAdjustmentRequest.class)))
                .thenReturn(productWithStock(120));

        mockMvc.perform(patch("/api/inventory/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(20, "delivery"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(120));
    }

    @Test
    void addStock_returns400_whenQuantityMissing() throws Exception {
        mockMvc.perform(patch("/api/inventory/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"delivery\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    void addStock_returns400_whenQuantityNotPositive() throws Exception {
        mockMvc.perform(patch("/api/inventory/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(0, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    void addStock_returns422_whenExceedsMax() throws Exception {
        when(inventoryService.addStock(eq(1L), any(StockAdjustmentRequest.class)))
                .thenThrow(new StockLimitExceededException("Widget", 490, 20, 500));

        mockMvc.perform(patch("/api/inventory/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(20, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void removeStock_returns200_whenValid() throws Exception {
        when(inventoryService.removeStock(eq(1L), any(StockAdjustmentRequest.class)))
                .thenReturn(productWithStock(80));

        mockMvc.perform(patch("/api/inventory/1/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(20, "damage"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(80));
    }

    @Test
    void removeStock_returns422_whenInsufficientStock() throws Exception {
        when(inventoryService.removeStock(eq(1L), any(StockAdjustmentRequest.class)))
                .thenThrow(new InsufficientStockException("Widget", 5, 20));

        mockMvc.perform(patch("/api/inventory/1/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(20, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void removeStock_returns404_whenProductMissing() throws Exception {
        when(inventoryService.removeStock(eq(99L), any(StockAdjustmentRequest.class)))
                .thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(patch("/api/inventory/99/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(5, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
