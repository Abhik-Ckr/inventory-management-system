package com.inventory.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SaleRequest(
        @NotEmpty(message = "A sale needs at least one item")
        @Valid List<SaleItemRequest> items
) {
}
