package com.dearlavion.storeengine.productitem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.util.List;

public record UpdateProductItemRequest(
        String brand,
        @Min(1) @Max(3) Integer sizeTier,
        String sizeLabel,
        String name,
        @Positive Double price,
        String currency,
        String image,
        @Size(max = 5) List<@URL String> images,
        @Size(max = 5) @Valid List<ProductVideoRequest> videos,
        String icon,
        @Min(0) Integer stock,
        Boolean soldOut,
        Boolean active,
        Boolean onSale,
        @Pattern(regexp = "percent|amount") String discountType,
        @Min(0) Double discountValue
) {
}
