package com.inventory.system.service;

import com.inventory.system.dto.SaleRequest;
import com.inventory.system.dto.SaleResponse;

import java.util.List;

public interface SaleService {
    SaleResponse createSale(SaleRequest request);
    SaleResponse getSale(Long id);
    List<SaleResponse> getAllSales();
}
