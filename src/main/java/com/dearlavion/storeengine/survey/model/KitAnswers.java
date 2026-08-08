package com.dearlavion.storeengine.survey.model;

import java.util.List;

/** Trip parameters the recommendation engine scores against. [] / null on destinations/activities/
 * priorityCategories means "All" — unrestricted, mirrors the tag-side convention (an untagged/
 * 'All'-tagged product fits any destination). */
public record KitAnswers(
        List<String> destinations,
        String season,
        String party,
        Integer partySize,
        // Stable code ('short'|'medium'|'long'), not the admin-editable display label.
        String duration,
        List<String> activities,
        String transportation,
        List<String> priorityCategories
) {
}
