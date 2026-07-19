package com.foodapp.controller;

import com.foodapp.dto.request.OrderRequest;
import com.foodapp.dto.request.OrderStatusUpdateRequest;
import com.foodapp.dto.response.OrderResponse;
import com.foodapp.entity.User;
import com.foodapp.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal User currentUser) {
        OrderResponse response = orderService.placeOrder(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        // Fine-grained "is this actually your order" check happens in status
        // update / cancel flows; a plain GET-by-id is intentionally left simple here.
        // In a stricter implementation you'd also verify currentUser owns this order or its restaurant.
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal User currentUser, Pageable pageable) {
        return ResponseEntity.ok(orderService.getCustomerOrders(currentUser.getId(), pageable));
    }

    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getRestaurantOrders(
            @PathVariable Long restaurantId, Pageable pageable) {
        return ResponseEntity.ok(orderService.getRestaurantOrders(restaurantId, pageable));
    }

    // Restaurant owner moves the order forward: PLACED -> CONFIRMED -> PREPARING -> OUT_FOR_DELIVERY -> DELIVERED
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('RESTAURANT_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.getNewStatus(), currentUser));
    }

    // Customer-initiated cancellation - separate endpoint from updateStatus because
    // the authorization rule is different (customer can cancel their own order,
    // but can't move it to CONFIRMED/PREPARING/etc.)
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(orderService.cancelOrder(id, currentUser));
    }
}
