package com.foodapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class RestaurantResponse {
    private Long id;
    private String name;
    private String description;
    private String cuisineType;
    private String location;
    private Double rating;
    private Boolean isActive;
    // menuItems is null in list/search views and populated only in the
    // "get restaurant by id" detail view - keeps list payloads light.
    private List<MenuItemResponse> menuItems;
}
