package com.inventory.system.exception;

/**
 * Raised when a sale references a product that has been deactivated
 * (discontinued). The catalog row still exists so it isn't a 404, but the
 * SKU can no longer be sold — a business-rule violation, mapped to 422.
 */
public class InactiveProductException extends RuntimeException {
    public InactiveProductException(String productName, String sku) {
        super("Product '" + productName + "' (SKU " + sku + ") is inactive and cannot be sold");
    }
}
