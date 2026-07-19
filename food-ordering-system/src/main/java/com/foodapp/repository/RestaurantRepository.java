package com.foodapp.repository;

import com.foodapp.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findByOwnerId(Long ownerId);

    // Derived query: case-insensitive partial match filters, combined with AND.
    // Pageable param -> Spring auto-applies LIMIT/OFFSET + ORDER BY from the request.
    Page<Restaurant> findByCuisineTypeContainingIgnoreCaseAndLocationContainingIgnoreCase(
            String cuisineType, String location, Pageable pageable);

    // Custom JPQL when the derived-query name would get too unwieldy.
    // :searchTerm matches restaurant name OR cuisine, case-insensitively.
    @Query("SELECT r FROM Restaurant r WHERE " +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.cuisineType) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Restaurant> search(@Param("searchTerm") String searchTerm, Pageable pageable);

    Optional<Restaurant> findByIdAndOwnerId(Long id, Long ownerId);
}
