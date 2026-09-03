package com.inventory.system.exception;

/**
 * Raised when adding stock would push quantity-on-hand above the product's
 * configured maxStockLevel (e.g. warehouse capacity). A business-rule
 * violation rather than bad input, so the handler maps it to 422.
 */
public class StockLimitExceededException extends RuntimeException {
    public StockLimitExceededException(String productName, int current, int requested, int maxStockLevel) {
        super("Adding " + requested + " unit(s) to '" + productName + "' would exceed the maximum stock level: "
                + "current " + current + ", max " + maxStockLevel);
    }
}
