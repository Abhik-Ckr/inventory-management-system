package com.inventory.system.controller;

import com.inventory.system.dto.ErrorResponse;
import com.inventory.system.dto.ProductRequest;
import com.inventory.system.dto.ProductResponse;
import com.inventory.system.dto.ProductUpdateRequest;
import com.inventory.system.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Products", description = "Create, read, update, search, and deactivate products in the catalog.")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Create a product",
            description = "Adds a new product to the catalog with its initial stock. The SKU must be unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Validation failed (e.g. blank SKU/name, negative price/quantity)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A product with the given SKU already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @Operation(summary = "List all products", description = "Returns every product in the catalog, active or not.")
    @ApiResponse(responseCode = "200", description = "List of products")
    @GetMapping
    public List<ProductResponse> getAll() {
        return productService.getAllProducts();
    }

    @Operation(summary = "Search products",
            description = "Case-insensitive partial match over name and SKU, active products only. The search term is required.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching products (may be empty)"),
            @ApiResponse(responseCode = "400", description = "Search term is missing or blank",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/search")
    public List<ProductResponse> search(
            @RequestParam("q") @NotBlank(message = "Search term is required") String query) {
        return productService.searchProducts(query);
    }

    @Operation(summary = "Get a product by id", description = "Returns a single product and its current stock.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The product"),
            @ApiResponse(responseCode = "404", description = "No product with the given id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @Operation(summary = "Update a product",
            description = "Partial update — only non-null fields in the body are applied. Use this to change price, category, reorder level, or to activate/deactivate.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated product"),
            @ApiResponse(responseCode = "400", description = "Validation failed (e.g. negative price)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No product with the given id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The product was modified concurrently; retry",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return productService.updateProduct(id, request);
    }

    @Operation(summary = "Deactivate a product",
            description = "Soft-deletes (deactivates) the product so it can no longer be sold. The catalog row is kept.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deactivated"),
            @ApiResponse(responseCode = "404", description = "No product with the given id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        productService.deactivateProduct(id);
        return ResponseEntity.noContent().build();
    }
}
