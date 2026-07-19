package com.foodapp.service;

import com.foodapp.dto.request.OrderItemRequest;
import com.foodapp.dto.request.OrderRequest;
import com.foodapp.dto.response.OrderItemResponse;
import com.foodapp.dto.response.OrderResponse;
import com.foodapp.entity.*;
import com.foodapp.exception.InvalidOrderStateException;
import com.foodapp.exception.ResourceNotFoundException;
import com.foodapp.exception.UnauthorizedActionException;
import com.foodapp.repository.MenuItemRepository;
import com.foodapp.repository.OrderRepository;
import com.foodapp.repository.PaymentRepository;
import com.foodapp.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final PaymentRepository paymentRepository;

    /**
     * THE STATE MACHINE.
     * Key = current status, Value = the set of statuses it is legally allowed to move to.
     * This single map is what "enforces" the state machine - transitionStatus() below
     * just checks membership in this map instead of a long if/else chain.
     *
     * PLACED           -> CONFIRMED, CANCELLED
     * CONFIRMED        -> PREPARING, CANCELLED
     * PREPARING        -> OUT_FOR_DELIVERY
     * OUT_FOR_DELIVERY -> DELIVERED
     * DELIVERED        -> (terminal, nothing allowed)
     * CANCELLED        -> (terminal, nothing allowed)
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);
    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.PLACED, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PREPARING, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY));
        ALLOWED_TRANSITIONS.put(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED));
        ALLOWED_TRANSITIONS.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request, User customer) {
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        Order order = Order.builder()
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.PLACED)
                .deliveryAddress(request.getDeliveryAddress())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Menu item not found: " + itemReq.getMenuItemId()));

            if (!Boolean.TRUE.equals(menuItem.getAvailable())) {
                throw new InvalidOrderStateException(
                        "'" + menuItem.getName() + "' is currently unavailable");
            }

            // Price is captured NOW, at order time - this is the "price snapshot"
            // mentioned on OrderItem. Even if the restaurant edits the price later
            // (which bumps MenuItem.version via optimistic locking), this order's
            // total stays historically accurate.
            BigDecimal lineTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .menuItem(menuItem)
                    .quantity(itemReq.getQuantity())
                    .priceAtOrderTime(menuItem.getPrice())
                    .build();

            order.getOrderItems().add(orderItem);
        }

        order.setTotalAmount(total);
        orderRepository.save(order);   // cascades and saves all OrderItems too (CascadeType.ALL on Order.orderItems)

        // Mock payment - in a real system this step would call an external gateway
        // and this Payment row would be created only after a webhook confirms success.
        Payment payment = Payment.builder()
                .order(order)
                .amount(total)
                .status(PaymentStatus.SUCCESS)
                .transactionRef(UUID.randomUUID().toString())
                .build();
        paymentRepository.save(payment);

        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Only the restaurant's owner (or an admin) can move the order forward;
        // a customer is only allowed to trigger CANCELLED (handled in cancelOrder()).
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isRestaurantOwner = order.getRestaurant().getOwner().getId().equals(currentUser.getId());
        if (!isAdmin && !isRestaurantOwner) {
            throw new UnauthorizedActionException("Only the restaurant owner can update this order's status");
        }

        transitionStatus(order, newStatus);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        boolean isCustomer = order.getCustomer().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isCustomer && !isAdmin) {
            throw new UnauthorizedActionException("You cannot cancel someone else's order");
        }

        transitionStatus(order, OrderStatus.CANCELLED);
        return toResponse(order);
    }

    /**
     * Core state machine enforcement. Throws InvalidOrderStateException
     * if the requested transition isn't in ALLOWED_TRANSITIONS.
     * Centralizing this in one method means no controller or service can
     * accidentally bypass the rules by setting order.setStatus() directly.
     */
    private void transitionStatus(Order order, OrderStatus newStatus) {
        Set<OrderStatus> allowedNext = ALLOWED_TRANSITIONS.get(order.getStatus());

        if (allowedNext == null || !allowedNext.contains(newStatus)) {
            throw new InvalidOrderStateException(
                    String.format("Cannot move order from %s to %s", order.getStatus(), newStatus));
        }

        order.setStatus(newStatus);
        // no explicit save() needed - dirty checking flushes this at transaction commit
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getCustomerOrders(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getRestaurantOrders(Long restaurantId, Pageable pageable) {
        return orderRepository.findByRestaurantId(restaurantId, pageable).map(this::toResponse);
    }

    private OrderResponse toResponse(Order order) {
        var items = order.getOrderItems().stream()
                .map(oi -> new OrderItemResponse(
                        oi.getMenuItem().getId(),
                        oi.getMenuItem().getName(),
                        oi.getQuantity(),
                        oi.getPriceAtOrderTime()))
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .restaurantId(order.getRestaurant().getId())
                .restaurantName(order.getRestaurant().getName())
                .items(items)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .deliveryAddress(order.getDeliveryAddress())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
