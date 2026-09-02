package com.inventory.system.service.impl;

import com.inventory.system.dto.ProductRequest;
import com.inventory.system.dto.ProductResponse;
import com.inventory.system.dto.ProductUpdateRequest;
import com.inventory.system.exception.DuplicateSkuException;
import com.inventory.system.exception.ProductNotFoundException;
import com.inventory.system.model.Inventory;
import com.inventory.system.model.Product;
import com.inventory.system.repository.InventoryRepository;
import com.inventory.system.repository.ProductRepository;
import com.inventory.system.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }

        Product product = Product.builder()
                .sku(request.sku())
                .name(request.name())
                .description(request.description())
                .category(request.category())
                .unitPrice(request.unitPrice())
                .active(true)
                .build();
        product = productRepository.save(product);

        Inventory inventory = Inventory.builder()
                .product(product)
                .quantityOnHand(request.quantityOnHand())
                .reorderLevel(request.reorderLevel() != null ? request.reorderLevel() : 0)
                .maxStockLevel(request.maxStockLevel())
                .build();
        inventory = inventoryRepository.save(inventory);

        return toResponse(product, inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = findProductOrThrow(id);
        Inventory inventory = findInventoryOrThrow(id);
        return toResponse(product, inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findByActiveTrue().stream()
                .map(p -> toResponse(p, findInventoryOrThrow(p.getId())))
                .toList();
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = findProductOrThrow(id);
        Inventory inventory = findInventoryOrThrow(id);

        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.category() != null) product.setCategory(request.category());
        if (request.unitPrice() != null) product.setUnitPrice(request.unitPrice());
        if (request.active() != null) product.setActive(request.active());
        if (request.reorderLevel() != null) inventory.setReorderLevel(request.reorderLevel());
        if (request.maxStockLevel() != null) inventory.setMaxStockLevel(request.maxStockLevel());

        product = productRepository.save(product);
        inventory = inventoryRepository.save(inventory);
        return toResponse(product, inventory);
    }

    @Override
    @Transactional
    public void deactivateProduct(Long id) {
        Product product = findProductOrThrow(id);
        product.setActive(false);
        productRepository.save(product);
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Inventory findInventoryOrThrow(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException("No inventory record for product id: " + productId));
    }

    private ProductResponse toResponse(Product product, Inventory inventory) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getUnitPrice(),
                product.isActive(),
                inventory.getQuantityOnHand(),
                inventory.getReorderLevel(),
                inventory.getMaxStockLevel(),
                inventory.isLowStock(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
