package com.dearlavion.storeengine.product.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** destinations/seasons/parties/activities/transportModes/kitCategory are plain strings, not a
 * closed enum — the option lists are admin-editable (see dearlavion-spring-master-data-service), so there's no fixed
 * set to validate against at compile time. The admin UI's dropdowns are the actual enforcement
 * point, not this DTO (matches how `category`, backed by the Category collection, is validated). */
public record CreateProductRequest(
        @NotBlank String name,
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
        String kitCategory,
        List<String> linkedProductIds
) {
}
