package com.inventory.system.controller;

import com.inventory.system.dto.ErrorResponse;
import com.inventory.system.dto.SaleRequest;
import com.inventory.system.dto.SaleResponse;
import com.inventory.system.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@Tag(name = "Sales", description = "Record sales and look up sale history. Recording a sale decrements stock.")
public class SaleController {

    private final SaleService saleService;

    @Operation(summary = "Record a sale",
            description = "Records a sale of one or more products and decrements their stock. "
                    + "Each product may appear only once, must be active, and must have enough stock.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sale recorded"),
            @ApiResponse(responseCode = "400", description = "Validation failed, or the same product appears more than once",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "A referenced product does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "A product is inactive, or there is insufficient stock",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.createSale(request));
    }

    @Operation(summary = "List all sales", description = "Returns every recorded sale with its line items.")
    @ApiResponse(responseCode = "200", description = "List of sales")
    @GetMapping
    public List<SaleResponse> getAll() {
        return saleService.getAllSales();
    }

    @Operation(summary = "Get a sale by id", description = "Returns a single recorded sale with its line items.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The sale"),
            @ApiResponse(responseCode = "404", description = "No sale with the given id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public SaleResponse getById(@PathVariable Long id) {
        return saleService.getSale(id);
    }
}
