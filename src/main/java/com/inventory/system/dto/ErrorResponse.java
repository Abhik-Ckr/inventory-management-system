package com.inventory.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Standard error body returned for every failed request.")
public record ErrorResponse(
        @Schema(description = "When the error occurred.", example = "2026-09-06T13:24:10Z") Instant timestamp,
        @Schema(description = "HTTP status code.", example = "422") int status,
        @Schema(description = "HTTP status label.", example = "Unprocessable Entity") String error,
        @Schema(description = "Human-readable error message.", example = "Insufficient stock for 'Mechanical Keyboard': available 3, requested 10") String message,
        @Schema(description = "Request path that produced the error.", example = "/api/sales") String path,
        @Schema(description = "Per-field validation errors, present only for request-body validation failures.",
                example = "{\"sku\":\"SKU is required\"}") Map<String, String> fieldErrors
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, null);
    }

    public ErrorResponse(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        this(Instant.now(), status, error, message, path, fieldErrors);
    }
}
