package com.inventory.system.exception;

/**
 * Raised when a single sale request lists the same product on more than one
 * line item. Each line is validated against stock independently, so a
 * duplicate would silently double-count — we reject it as malformed input (400).
 */
public class DuplicateSaleItemException extends RuntimeException {
    public DuplicateSaleItemException(Long productId) {
        super("Product id " + productId + " appears more than once in the sale; combine it into a single line item");
    }
}
