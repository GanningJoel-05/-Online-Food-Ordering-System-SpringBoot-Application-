package com.foodapp.service;

import com.foodapp.dto.request.RestaurantRequest;
import com.foodapp.dto.response.MenuItemResponse;
import com.foodapp.dto.response.RestaurantResponse;
import com.foodapp.entity.Restaurant;
import com.foodapp.entity.User;
import com.foodapp.exception.ResourceNotFoundException;
import com.foodapp.exception.UnauthorizedActionException;
import com.foodapp.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    @Transactional
    public RestaurantResponse createRestaurant(RestaurantRequest request, User owner) {
        Restaurant restaurant = Restaurant.builder()
                .name(request.getName())
                .description(request.getDescription())
                .cuisineType(request.getCuisineType())
                .location(request.getLocation())
                .owner(owner)
                .build();

        restaurantRepository.save(restaurant);
        return toResponse(restaurant);
    }

    // @Transactional(readOnly = true): tells Hibernate this method never writes,
    // enabling read-only query optimizations, and keeps the persistence context
    // open long enough to safely access lazy collections (menuItems) before the DTO mapping.
    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurantById(Long id) {
        Restaurant restaurant = findByIdOrThrow(id);
        return toResponseWithMenu(restaurant);
    }

    @Transactional(readOnly = true)
    public Page<RestaurantResponse> searchRestaurants(String searchTerm, Pageable pageable) {
        Page<Restaurant> page = (searchTerm == null || searchTerm.isBlank())
                ? restaurantRepository.findAll(pageable)
                : restaurantRepository.search(searchTerm, pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public RestaurantResponse updateRestaurant(Long id, RestaurantRequest request, User currentUser) {
        Restaurant restaurant = findByIdOrThrow(id);
        assertOwnership(restaurant, currentUser);

        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setCuisineType(request.getCuisineType());
        restaurant.setLocation(request.getLocation());
        // no explicit save() call needed: this method runs inside @Transactional,
        // so Hibernate's "dirty checking" auto-flushes the changed entity to the DB
        // at commit time. Save() is only required for brand-new entities.

        return toResponse(restaurant);
    }

    @Transactional
    public void deleteRestaurant(Long id, User currentUser) {
        Restaurant restaurant = findByIdOrThrow(id);
        assertOwnership(restaurant, currentUser);
        restaurantRepository.delete(restaurant);
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getMyRestaurants(Long ownerId) {
        return restaurantRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ---- helpers ----

    Restaurant findByIdOrThrow(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
    }

    private void assertOwnership(Restaurant restaurant, User currentUser) {
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isOwner = restaurant.getOwner().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new UnauthorizedActionException("You do not own this restaurant");
        }
    }

    private RestaurantResponse toResponse(Restaurant r) {
        return new RestaurantResponse(r.getId(), r.getName(), r.getDescription(), r.getCuisineType(),
                r.getLocation(), r.getRating(), r.getIsActive(), null);
    }

    private RestaurantResponse toResponseWithMenu(Restaurant r) {
        List<MenuItemResponse> items = r.getMenuItems().stream()
                .map(m -> new MenuItemResponse(m.getId(), m.getName(), m.getDescription(),
                        m.getPrice(), m.getCategory(), m.getAvailable()))
                .toList();
        return new RestaurantResponse(r.getId(), r.getName(), r.getDescription(), r.getCuisineType(),
                r.getLocation(), r.getRating(), r.getIsActive(), items);
    }
}
