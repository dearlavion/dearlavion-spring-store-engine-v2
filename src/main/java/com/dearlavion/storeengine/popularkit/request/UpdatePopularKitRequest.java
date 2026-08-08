package com.dearlavion.storeengine.popularkit.request;

import java.util.List;

public record UpdatePopularKitRequest(
        String name,
        String tag,
        String image,
        String destination,
        String season,
        String party,
        String duration,
        List<String> productIds
) {
}
