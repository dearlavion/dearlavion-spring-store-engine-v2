package com.dearlavion.storeengine.popularkit;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreatePopularKitRequest(
        @NotBlank String name,
        String tag,
        String image,
        String destination,
        String season,
        String party,
        String duration,
        List<String> productIds
) {
}
