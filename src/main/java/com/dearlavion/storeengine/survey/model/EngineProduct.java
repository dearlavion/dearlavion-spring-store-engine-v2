package com.dearlavion.storeengine.survey.model;

import java.util.List;

/** Minimal product shape the engine needs (mapped from a Product entity). */
public record EngineProduct(
        String id,
        String name,
        String category,
        List<String> destinations,
        List<String> seasons,
        List<String> parties,
        List<String> activities,
        List<String> transportModes,
        List<String> durations,
        List<String> genders,
        List<String> kitCategories,
        boolean popular,
        boolean tested
) {
}
