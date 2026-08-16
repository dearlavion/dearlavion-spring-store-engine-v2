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
        List<String> durations,
        List<String> genders,
        List<String> kitCategories,
        Boolean active,
        List<String> linkedProductIds
) {
}
