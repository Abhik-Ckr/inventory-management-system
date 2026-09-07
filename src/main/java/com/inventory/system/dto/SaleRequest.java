package com.inventory.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Payload for recording a sale of one or more products.")
public record SaleRequest(
        @Schema(description = "Line items in the sale. At least one is required, and a product may not appear twice.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "A sale needs at least one item")
        @Valid List<SaleItemRequest> items
) {
}
