package com.foodapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;    // BigDecimal, never double/float, for money - avoids rounding errors

    private String category;     // "Starters", "Main Course", "Desserts" etc.

    @Builder.Default
    private Boolean available = true;

    // @Version enables OPTIMISTIC LOCKING.
    // Hibernate auto-increments this on every UPDATE and checks it in the WHERE clause.
    // If two requests try to update the same row concurrently (e.g. owner marks item
    // unavailable while a customer's checkout reads it), the second write throws
    // OptimisticLockException instead of silently overwriting -> great interview talking point.
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;
}
