package com.example.eventlogpipeline.exception;

import java.util.UUID;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(UUID productId) {
        super("Out of stock: " + productId);
    }
}