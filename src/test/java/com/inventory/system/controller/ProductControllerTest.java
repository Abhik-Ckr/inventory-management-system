package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.dto.ProductRequest;
import com.inventory.system.dto.ProductResponse;
import com.inventory.system.exception.DuplicateSkuException;
import com.inventory.system.exception.ProductNotFoundException;
import com.inventory.system.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer unit tests for {@link ProductController}. The service is mocked,
 * so these exercise request mapping, validation, status codes, and JSON
 * serialization only — not the business logic.
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductResponse sampleResponse() {
        return new ProductResponse(1L, "SKU-001", "Widget", "A widget", "Hardware",
                BigDecimal.valueOf(9.99), true, 100, 10, 500, false,
                Instant.parse("2026-09-01T09:30:00Z"), Instant.parse("2026-09-05T14:12:00Z"));
    }

    @Test
    void create_returns201WithBody_whenRequestValid() throws Exception {
        ProductRequest request = new ProductRequest("SKU-001", "Widget", "A widget", "Hardware",
                BigDecimal.valueOf(9.99), 100, 10, 500);
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.quantityOnHand").value(100));
    }

    @Test
    void create_returns400_whenSkuBlank() throws Exception {
        ProductRequest request = new ProductRequest("  ", "Widget", null, null,
                BigDecimal.TEN, 5, 0, null);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.sku").exists());
    }

    @Test
    void create_returns400_whenUnitPriceNegative() throws Exception {
        ProductRequest request = new ProductRequest("SKU-9", "Widget", null, null,
                BigDecimal.valueOf(-1), 5, 0, null);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.unitPrice").exists());
    }

    @Test
    void create_returns409_whenSkuDuplicate() throws Exception {
        ProductRequest request = new ProductRequest("SKU-001", "Widget", null, null,
                BigDecimal.TEN, 5, 0, null);
        when(productService.createProduct(any(ProductRequest.class)))
                .thenThrow(new DuplicateSkuException("SKU-001"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SKU-001")));
    }

    @Test
    void getAll_returnsList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].sku").value("SKU-001"));
    }

    @Test
    void getById_returns200_whenFound() throws Exception {
        when(productService.getProduct(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_returns404_whenMissing() throws Exception {
        when(productService.getProduct(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void search_returns200_whenTermProvided() throws Exception {
        when(productService.searchProducts("wid")).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/products/search").param("q", "wid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Widget"));
    }

    @Test
    void search_returns400_whenTermBlank() throws Exception {
        mockMvc.perform(get("/api/products/search").param("q", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void update_returns200AndDelegates() throws Exception {
        when(productService.updateProduct(eq(1L), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitPrice\": 12.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(productService).updateProduct(eq(1L), any());
    }

    @Test
    void update_returns400_whenUnitPriceNegative() throws Exception {
        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitPrice\": -5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.unitPrice").exists());
    }

    @Test
    void update_returns409_whenOptimisticLockingFails() throws Exception {
        // A concurrent modification bumps the @Version, so the losing write
        // throws OptimisticLockingFailureException, which must map to 409.
        when(productService.updateProduct(eq(1L), any()))
                .thenThrow(new OptimisticLockingFailureException("stale"));

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitPrice\": 12.50}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deactivate_returns204() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deactivateProduct(1L);
    }
}
