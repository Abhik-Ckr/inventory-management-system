package com.inventory.system.service;

import com.inventory.system.dto.ProductRequest;
import com.inventory.system.dto.ProductResponse;
import com.inventory.system.dto.ProductUpdateRequest;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse getProduct(Long id);
    List<ProductResponse> getAllProducts();
    ProductResponse updateProduct(Long id, ProductUpdateRequest request);
    void deactivateProduct(Long id);
}
