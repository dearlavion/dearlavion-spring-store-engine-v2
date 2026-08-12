package com.dearlavion.storeengine.survey.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/** destinations/activities/priorityCategories: optional + array — [] or omitted = "All"
 * (unrestricted), matching the frontend's own convention and the tag-side 'All' value products
 * already carry. `duration` is the one exception: it's validated against a fixed, hardcoded set of
 * stable codes, because Duration's cardinality is NOT admin-editable (see AdminDurationController in
 * dearlavion-spring-master-data-service)
 * — the frontend sends this code (not the admin-editable display label), so KitEngine's kit-size
 * lookup can never break even if an admin renames "Quick escape" to something else. */
public record SurveyAnswersRequest(
        List<String> destinations,
        @NotBlank String season,
        @NotBlank String party,
        @Min(1) @Max(12) Integer partySize,
        @NotBlank @Pattern(regexp = "day|short|medium|long") String duration,
        List<String> activities,
        String transportation,
        String gender,
        List<String> priorityCategories
) {
}
