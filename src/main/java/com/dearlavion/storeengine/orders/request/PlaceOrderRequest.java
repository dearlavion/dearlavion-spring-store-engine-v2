package com.dearlavion.storeengine.orders.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PlaceOrderRequest(
        @NotEmpty @Valid List<OrderItemRequest> items,
        @Valid ShippingRequest shipping,
        @Min(0) double total,
        @Min(0) Double shippingFee,
        String currency,
        String reference,
        @Min(0) Double chargedAmount,
        String chargedCurrency
) {
}
