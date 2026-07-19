package com.foodapp.entity;

/**
 * User roles for role-based access control (RBAC).
 * Used in @PreAuthorize checks and JWT claims.
 */
public enum Role {
    CUSTOMER,          // browses restaurants, places orders
    RESTAURANT_OWNER,  // manages their own restaurant + menu + incoming orders
    ADMIN               // full access, can manage any restaurant/user
}
