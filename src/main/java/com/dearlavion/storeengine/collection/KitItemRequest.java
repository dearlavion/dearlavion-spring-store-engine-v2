package com.dearlavion.storeengine.collection;

import jakarta.validation.constraints.NotBlank;

public record KitItemRequest(@NotBlank String label, @NotBlank String productId) {
}
