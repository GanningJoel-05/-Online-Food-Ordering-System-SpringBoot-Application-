package com.foodapp.service;

import com.foodapp.dto.request.MenuItemRequest;
import com.foodapp.dto.response.MenuItemResponse;
import com.foodapp.entity.MenuItem;
import com.foodapp.entity.Restaurant;
import com.foodapp.entity.User;
import com.foodapp.exception.ResourceNotFoundException;
import com.foodapp.exception.UnauthorizedActionException;
import com.foodapp.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantService restaurantService;   // reused for lookup + ownership check

    @Transactional
    public MenuItemResponse addMenuItem(Long restaurantId, MenuItemRequest request, User currentUser) {
        Restaurant restaurant = restaurantService.findByIdOrThrow(restaurantId);
        assertOwnership(restaurant, currentUser);

        MenuItem item = MenuItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .available(request.getAvailable())
                .restaurant(restaurant)
                .build();

        menuItemRepository.save(item);
        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenuForRestaurant(Long restaurantId) {
        return menuItemRepository.findByRestaurantId(restaurantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long restaurantId, Long itemId, MenuItemRequest request, User currentUser) {
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        assertOwnership(item.getRestaurant(), currentUser);

        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(request.getCategory());
        item.setAvailable(request.getAvailable());
        // Note: item.getVersion() is bumped automatically by Hibernate on this UPDATE.
        // If another transaction read this same row and tries to save a stale version,
        // that write fails with OptimisticLockException.

        return toResponse(item);
    }

    @Transactional
    public void deleteMenuItem(Long restaurantId, Long itemId, User currentUser) {
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        assertOwnership(item.getRestaurant(), currentUser);
        menuItemRepository.delete(item);
    }

    private void assertOwnership(Restaurant restaurant, User currentUser) {
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isOwner = restaurant.getOwner().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new UnauthorizedActionException("You do not own this restaurant's menu");
        }
    }

    private MenuItemResponse toResponse(MenuItem m) {
        return new MenuItemResponse(m.getId(), m.getName(), m.getDescription(), m.getPrice(),
                m.getCategory(), m.getAvailable());
    }
}
