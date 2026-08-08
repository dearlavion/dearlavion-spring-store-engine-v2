package com.dearlavion.storeengine.collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BuiltKitRequest(
        @NotNull @Valid List<KitItemRequest> items,
        String summary,
        String title
) {
}
