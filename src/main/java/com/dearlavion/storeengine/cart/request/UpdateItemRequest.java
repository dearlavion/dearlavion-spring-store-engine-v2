package com.dearlavion.storeengine.cart.request;

import jakarta.validation.constraints.Min;

public record UpdateItemRequest(
        @Min(0) int quantity
) {
}
