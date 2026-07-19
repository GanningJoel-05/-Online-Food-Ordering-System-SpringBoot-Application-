package com.foodapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String cuisineType;     // e.g. "Italian", "North Indian" - used for search/filter

    @Column(nullable = false)
    private String location;        // simple string for medium-level scope; could be normalized later

    @Builder.Default
    private Double rating = 0.0;

    @Builder.Default
    private Boolean isActive = true;  // owner can temporarily close the restaurant

    // Many restaurants can be owned by one user in theory, but here we keep it 1:1 per owner for simplicity.
    // FetchType.LAZY = don't load the owner row unless .getOwner() is actually called -> avoids N+1 on restaurant lists
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // mappedBy = "restaurant" means Restaurant is NOT the owning side of this relationship;
    // the foreign key lives in the menu_items table (see MenuItem.restaurant)
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MenuItem> menuItems = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
