package com.inventory.system.controller;

import com.inventory.system.dto.AvailabilityResponse;
import com.inventory.system.dto.ErrorResponse;
import com.inventory.system.dto.ProductResponse;
import com.inventory.system.dto.StockAdjustmentRequest;
import com.inventory.system.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Validated
@Tag(name = "Inventory", description = "Check availability and adjust stock levels for a product.")
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "Check stock availability",
            description = "Checks whether the requested quantity of a product can be fulfilled from current stock. "
                    + "A 200 response always means it is available; insufficient stock returns an error instead.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The requested quantity is available"),
            @ApiResponse(responseCode = "400", description = "Quantity is not positive",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No product with the given id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{productId}/availability")
    public AvailabilityResponse checkAvailability(
            @PathVariable Long productId,
            @RequestParam @Min(value = 1, message = "Quantity must be positive") int quantity) {
        return inventoryService.checkAvailability(productId, quantity);
    }

    @Operation(summary = "Add stock",
            description = "Increases a product's quantity on hand. Fails if it would exceed the configured maximum stock level.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated product with new stock level"),
            @ApiResponse(responseCode = "400", description = "Quantity is missing or not positive",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No product with the given id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Adding this stock would exceed the product's maximum stock level",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{productId}/add")
    public ProductResponse addStock(@PathVariable Long productId, @Valid @RequestBody StockAdjustmentRequest request) {
        return inventoryService.addStock(productId, request);
    }

    @Operation(summary = "Remove stock",
            description = "Decreases a product's quantity on hand (e.g. shrinkage, damage). Fails if it would drop below zero.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated product with new stock level"),
            @ApiResponse(responseCode = "400", description = "Quantity is missing or not positive",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No product with the given id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Not enough stock on hand to remove the requested quantity",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{productId}/remove")
    public ProductResponse removeStock(@PathVariable Long productId, @Valid @RequestBody StockAdjustmentRequest request) {
        return inventoryService.removeStock(productId, request);
    }
}
