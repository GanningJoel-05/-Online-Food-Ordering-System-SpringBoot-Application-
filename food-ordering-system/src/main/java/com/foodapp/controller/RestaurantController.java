package com.foodapp.controller;

import com.foodapp.dto.request.RestaurantRequest;
import com.foodapp.dto.response.RestaurantResponse;
import com.foodapp.entity.User;
import com.foodapp.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    // Public - no auth required (see SecurityConfig: GET /api/restaurants/** is permitAll)
    @GetMapping
    public ResponseEntity<Page<RestaurantResponse>> searchRestaurants(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        // Pageable is auto-bound from query params like ?page=0&size=10&sort=rating,desc
        return ResponseEntity.ok(restaurantService.searchRestaurants(search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getRestaurantById(id));
    }

    // @PreAuthorize evaluates a SpEL expression against the current authentication
    // BEFORE the method body runs. hasRole('X') checks for a "ROLE_X" granted authority -
    // which is exactly what User.getAuthorities() produces.
    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<RestaurantResponse> createRestaurant(
            @Valid @RequestBody RestaurantRequest request,
            @AuthenticationPrincipal User currentUser) {
        // @AuthenticationPrincipal injects the User object that JwtAuthFilter placed
        // into the SecurityContext - no manual lookup needed.
        RestaurantResponse response = restaurantService.createRestaurant(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-restaurants")
    @PreAuthorize("hasRole('RESTAURANT_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<RestaurantResponse>> getMyRestaurants(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(restaurantService.getMyRestaurants(currentUser.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<RestaurantResponse> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantRequest request,
            @AuthenticationPrincipal User currentUser) {
        // Ownership (is this owner's restaurant?) is double-checked inside the service layer,
        // not just here - never trust authorization logic to live only in the controller.
        return ResponseEntity.ok(restaurantService.updateRestaurant(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        restaurantService.deleteRestaurant(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
