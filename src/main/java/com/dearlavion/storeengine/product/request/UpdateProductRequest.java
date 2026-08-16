package com.dearlavion.storeengine.product.request;

import java.util.List;
import java.util.Map;

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
        Map<String, List<String>> tags,
        Boolean active,
        List<String> linkedProductIds
) {
}
