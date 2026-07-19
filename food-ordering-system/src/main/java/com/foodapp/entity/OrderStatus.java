package com.foodapp.entity;

/**
 * Represents every state an Order can be in.
 * This enum is the backbone of the "state machine" feature —
 * see OrderService.transitionStatus() for the actual transition rules.
 */
public enum OrderStatus {
    PLACED,           // customer just checked out
    CONFIRMED,        // restaurant accepted the order
    PREPARING,        // food is being prepared
    OUT_FOR_DELIVERY, // handed to delivery partner
    DELIVERED,        // terminal state - success
    CANCELLED         // terminal state - order cancelled (by customer or system)
}
