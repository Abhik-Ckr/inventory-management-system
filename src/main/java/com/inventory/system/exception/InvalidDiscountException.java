package com.inventory.system.exception;

public class InvalidDiscountException extends RuntimeException {

    public InvalidDiscountException(String productName) {
        super("Discount cannot exceed the total price for product '" + productName + "'");
    }
}