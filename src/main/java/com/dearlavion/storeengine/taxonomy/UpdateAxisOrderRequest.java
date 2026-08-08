package com.dearlavion.storeengine.taxonomy;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateAxisOrderRequest(@NotEmpty List<String> order) {
}
