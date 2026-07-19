package com.foodapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class OrderItemResponse {
    private Long menuItemId;
    private String menuItemName;
    private int quantity;
    private BigDecimal priceAtOrderTime;
}
