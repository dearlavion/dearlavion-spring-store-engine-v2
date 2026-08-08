package com.dearlavion.storeengine.collection.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveKitRequest(@NotBlank String name, @NotNull @Valid BuiltKitRequest kit) {
}
