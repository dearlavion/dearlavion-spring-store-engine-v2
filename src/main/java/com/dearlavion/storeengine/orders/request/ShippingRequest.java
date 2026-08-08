package com.dearlavion.storeengine.orders.request;

import jakarta.validation.constraints.NotBlank;

public record ShippingRequest(
        @NotBlank String fullName,
        @NotBlank String email,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String postalCode
) {
}
