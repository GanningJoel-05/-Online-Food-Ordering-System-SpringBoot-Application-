package com.foodapp.exception;

// Thrown whenever a fetch-by-id fails (restaurant, menu item, order, user not found)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
