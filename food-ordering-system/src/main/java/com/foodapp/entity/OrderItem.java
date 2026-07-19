package com.foodapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Join entity between Order and MenuItem, with extra fields (quantity, price).
 * We can't use a plain @ManyToMany here because we need to store quantity
 * and a PRICE SNAPSHOT (see priceAtOrderTime below) - a plain many-to-many
 * join table can't hold that extra data.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false)
    private Integer quantity;

    // IMPORTANT: we snapshot the price at the moment of ordering.
    // If the restaurant changes the menu price tomorrow, past orders
    // must still show what the customer actually paid.
    @Column(nullable = false)
    private BigDecimal priceAtOrderTime;
}
