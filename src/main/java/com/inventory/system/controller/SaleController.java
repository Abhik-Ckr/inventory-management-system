package com.inventory.system.controller;

import com.inventory.system.dto.SaleRequest;
import com.inventory.system.dto.SaleResponse;
import com.inventory.system.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @PostMapping
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.createSale(request));
    }

    @GetMapping
    public List<SaleResponse> getAll() {
        return saleService.getAllSales();
    }

    @GetMapping("/{id}")
    public SaleResponse getById(@PathVariable Long id) {
        return saleService.getSale(id);
    }
}
