package com.dearlavion.storeengine.taxonomy;

import com.dearlavion.storeengine.common.exception.ConflictException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Admin management of the 8 axes. Duration is a fixed-cardinality axis (exactly 3 rows, seeded
 * with an immutable `code` kit-engine keys its kit-size lookup on) — add/delete are rejected for
 * it here; only renaming an existing row's value/subtext is allowed, via update(). */
@RestController
@RequestMapping("/admin/taxonomies")
@RequiredArgsConstructor
public class AdminTaxonomyController {

    private final TaxonomyService taxonomies;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaxonomyValue create(@Valid @RequestBody CreateTaxonomyValueRequest body) {
        if ("duration".equals(body.axis())) {
            throw new ConflictException("Duration has a fixed set of options — edit the existing ones instead of adding new ones");
        }
        return taxonomies.create(body);
    }

    // Unlike NestJS/Express, Spring MVC resolves ambiguous mappings by specificity, not declaration
    // order — a literal "/axis-order" segment always wins over "/{id}" regardless of method order.
    // Kept as a separate method purely for readability, mirroring the NestJS source's structure.
    @PutMapping("/axis-order")
    public Map<String, List<String>> updateAxisOrder(@Valid @RequestBody UpdateAxisOrderRequest body) {
        return Map.of("order", taxonomies.updateAxisOrder(body.order()));
    }

    @PutMapping("/{id}")
    public TaxonomyValue update(@PathVariable String id, @RequestBody UpdateTaxonomyValueRequest body) {
        return taxonomies.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String id) {
        TaxonomyValue row = taxonomies.findById(id);
        if (row != null && "duration".equals(row.getAxis())) {
            throw new ConflictException("Duration has a fixed set of options and cannot be deleted");
        }
        taxonomies.delete(id);
    }
}
