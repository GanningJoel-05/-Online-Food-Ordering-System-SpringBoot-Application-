package com.foodapp.exception;

// Thrown on registration when the email is already taken
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
