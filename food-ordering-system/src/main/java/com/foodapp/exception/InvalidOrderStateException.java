package com.foodapp.exception;

// Thrown by the order state machine when an illegal status transition is attempted
// e.g. trying to move a DELIVERED order back to PLACED
public class InvalidOrderStateException extends RuntimeException {
    public InvalidOrderStateException(String message) {
        super(message);
    }
}
