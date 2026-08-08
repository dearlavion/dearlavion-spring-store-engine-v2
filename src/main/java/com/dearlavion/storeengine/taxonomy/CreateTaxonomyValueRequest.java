package com.dearlavion.storeengine.taxonomy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateTaxonomyValueRequest(
        @NotBlank
        @Pattern(regexp = "destination|season|party|transportation|activity|kitCategory|duration|gender")
        String axis,
        @NotBlank String value,
        Integer order,
        String emoji,
        String subtext
) {
}
