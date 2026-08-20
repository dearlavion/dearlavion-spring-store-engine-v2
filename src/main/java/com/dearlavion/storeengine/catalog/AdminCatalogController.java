package com.dearlavion.storeengine.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Lets an admin see how old the cached catalog is and force a reload.
 *
 * <p>The write paths already refresh on every product/item change, so this is for changes the
 * application didn't make — a migration, an edit straight in Atlas, the seeder — and as the
 * first thing to try when survey results look wrong.
 *
 * <p>Exposing {@code builtAt} matters as much as the refresh itself: a cache whose staleness is
 * invisible is the failure mode that costs hours.
 */
@RestController
@RequestMapping("/admin/catalog")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final CatalogCache catalog;

    public record CatalogStatus(Instant builtAt, int products, int items, boolean cached) {
    }

    @GetMapping("/status")
    public CatalogStatus status() {
        return describe(catalog.get());
    }

    @PostMapping("/refresh")
    public CatalogStatus refresh() {
        return describe(catalog.refresh());
    }

    private CatalogStatus describe(CatalogSnapshot snapshot) {
        return new CatalogStatus(snapshot.builtAt(), snapshot.productCount(), snapshot.itemCount(),
                catalog instanceof InMemoryCatalogCache);
    }
}
