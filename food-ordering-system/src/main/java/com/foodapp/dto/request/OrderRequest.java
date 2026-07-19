package com.foodapp.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    @NotNull
    private Long restaurantId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid   // cascades validation into each OrderItemRequest in the list
    private List<OrderItemRequest> items;

    private String deliveryAddress;
}
