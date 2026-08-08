package com.dearlavion.storeengine.shippingdetails;

import jakarta.validation.constraints.NotBlank;

public record SaveShippingDetailsRequest(
        @NotBlank String fullName,
        @NotBlank String email,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String postalCode
) {
}
