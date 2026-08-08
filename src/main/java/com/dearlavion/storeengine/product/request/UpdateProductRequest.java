package com.dearlavion.storeengine.product.request;

import java.util.List;

public record UpdateProductRequest(
        String name,
        String category,
        String description,
        String icon,
        Boolean popular,
        Boolean tested,
        List<String> destinations,
        List<String> seasons,
        List<String> parties,
        List<String> activities,
        List<String> transportModes,
        String kitCategory,
        Boolean active,
        List<String> linkedProductIds
) {
}
