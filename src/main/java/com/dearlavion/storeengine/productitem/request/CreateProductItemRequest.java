package com.dearlavion.storeengine.productitem.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.util.List;

public record CreateProductItemRequest(
        @NotBlank String productId,
        String brand,
        @Min(1) @Max(3) Integer sizeTier,
        String sizeLabel,
        @NotBlank String name,
        @Positive double price,
        String currency,
        String image,
        @Size(max = 5) List<@URL String> images,
        @Size(max = 5) @Valid List<ProductVideoRequest> videos,
        String icon,
        @Min(0) Integer stock,
        Boolean soldOut,
        Boolean onSale,
        @Pattern(regexp = "percent|amount") String discountType,
        @Min(0) Double discountValue
) {
}
