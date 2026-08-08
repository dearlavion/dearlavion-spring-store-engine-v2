package com.dearlavion.storeengine.taxonomy;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Public: every taxonomy value, every axis — backs the /travel survey, Shop's filter chips, and
 * the admin product form's dropdowns (all public-facing reads, same as GET /categories). */
@RestController
@RequiredArgsConstructor
public class TaxonomyController {

    private final TaxonomyService taxonomies;

    // Declared as its own explicit method (not a path variable on GET /taxonomies/{id}, which
    // doesn't exist) — no route-ordering ambiguity risk in Spring MVC the way NestJS's decorator
    // order matters, but kept as a distinct mapping for clarity.
    @GetMapping("/taxonomies/axis-order")
    public Map<String, List<String>> getAxisOrder() {
        return Map.of("order", taxonomies.getAxisOrder());
    }

    @GetMapping("/taxonomies")
    public List<TaxonomyValue> list() {
        return taxonomies.listAll();
    }
}
