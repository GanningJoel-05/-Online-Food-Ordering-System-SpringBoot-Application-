package com.foodapp.exception;

// Thrown when a user tries to act on a resource they don't own
// e.g. a restaurant owner trying to edit another owner's menu
public class UnauthorizedActionException extends RuntimeException {
    public UnauthorizedActionException(String message) {
        super(message);
    }
}
