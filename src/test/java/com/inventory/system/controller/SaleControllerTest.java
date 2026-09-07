package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.dto.SaleItemRequest;
import com.inventory.system.dto.SaleItemResponse;
import com.inventory.system.dto.SaleRequest;
import com.inventory.system.dto.SaleResponse;
import com.inventory.system.exception.DuplicateSaleItemException;
import com.inventory.system.exception.InactiveProductException;
import com.inventory.system.exception.InsufficientStockException;
import com.inventory.system.exception.ProductNotFoundException;
import com.inventory.system.exception.SaleNotFoundException;
import com.inventory.system.service.SaleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer unit tests for {@link SaleController} with a mocked service.
 */
@WebMvcTest(SaleController.class)
class SaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SaleService saleService;

    private SaleResponse sampleSale() {
        SaleItemResponse item = new SaleItemResponse(1L, "SKU-001", "Widget", 2,
                BigDecimal.valueOf(10), BigDecimal.ZERO, BigDecimal.valueOf(20));
        return new SaleResponse(100L, Instant.parse("2026-09-05T14:20:00Z"),
                BigDecimal.valueOf(20), "COMPLETED", List.of(item));
    }

    @Test
    void create_returns201_whenValid() throws Exception {
        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(1L, 2, null)));
        when(saleService.createSale(any(SaleRequest.class))).thenReturn(sampleSale());

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void create_returns400_whenNoItems() throws Exception {
        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.items").exists());
    }

    @Test
    void create_returns400_whenItemQuantityNotPositive() throws Exception {
        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(1L, 0, null)));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenDiscountNegative() throws Exception {
        SaleRequest request = new SaleRequest(List.of(
                new SaleItemRequest(1L, 1, BigDecimal.valueOf(-1))));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenDuplicateItems() throws Exception {
        SaleRequest request = new SaleRequest(List.of(
                new SaleItemRequest(1L, 1, null), new SaleItemRequest(1L, 2, null)));
        when(saleService.createSale(any(SaleRequest.class)))
                .thenThrow(new DuplicateSaleItemException(1L));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void create_returns404_whenProductMissing() throws Exception {
        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(99L, 1, null)));
        when(saleService.createSale(any(SaleRequest.class)))
                .thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void create_returns422_whenInactiveProduct() throws Exception {
        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(1L, 1, null)));
        when(saleService.createSale(any(SaleRequest.class)))
                .thenThrow(new InactiveProductException("Widget", "SKU-001"));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void create_returns422_whenInsufficientStock() throws Exception {
        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(1L, 100, null)));
        when(saleService.createSale(any(SaleRequest.class)))
                .thenThrow(new InsufficientStockException("Widget", 10, 100));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void getAll_returnsList() throws Exception {
        when(saleService.getAllSales()).thenReturn(List.of(sampleSale()));

        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(100));
    }

    @Test
    void getById_returns200_whenFound() throws Exception {
        when(saleService.getSale(100L)).thenReturn(sampleSale());

        mockMvc.perform(get("/api/sales/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    void getById_returns404_whenMissing() throws Exception {
        when(saleService.getSale(404L)).thenThrow(new SaleNotFoundException(404L));

        mockMvc.perform(get("/api/sales/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
