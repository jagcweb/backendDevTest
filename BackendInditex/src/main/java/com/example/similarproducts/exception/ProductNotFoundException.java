package com.example.similarproducts.exception;

/**
 * Raised when the upstream catalog has no knowledge of the requested product, which per the
 * agreed contract should surface to the caller as a 404.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String productId) {
        super("Product not found: " + productId);
    }
}
