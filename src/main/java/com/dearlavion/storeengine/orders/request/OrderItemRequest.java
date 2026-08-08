package com.dearlavion.storeengine.orders.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderItemRequest(
        @NotBlank String productId,
        String productItemId,
        String brand,
        @NotBlank String name,
        String icon,
        @Min(1) int quantity,
        @Min(0) double price,
        String currency
) {
}
