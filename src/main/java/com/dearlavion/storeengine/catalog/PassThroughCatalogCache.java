package com.dearlavion.storeengine.catalog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Loads the catalog from Mongo on every call — the behaviour SurveyService had before any caching
 * existed, kept as a supported mode rather than as dead code.
 *
 * <p>A cache implementation that deliberately doesn't cache looks odd, but it means the toggle lives
 * entirely in configuration: callers depend on {@link CatalogCache} and never learn which mode is
 * active. It is the escape hatch if cached data is ever suspected of being wrong, and the reference
 * implementation to diff against when checking that caching changed nothing.
 *
 * <p>Active when {@code app.catalog-cache-enabled} is false.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.catalog-cache-enabled", havingValue = "false")
@RequiredArgsConstructor
public class PassThroughCatalogCache implements CatalogCache {

    private final CatalogSnapshotLoader loader;

    @PostConstruct
    void announce() {
        log.info("Catalog cache DISABLED (app.catalog-cache-enabled=false) — every survey reads Mongo.");
    }

    /** Two Mongo round trips per call. Always current, never stale. */
    @Override
    public CatalogSnapshot get() {
        return loader.load();
    }

    /** Nothing is held, so there is nothing to invalidate; returns a freshly loaded snapshot. */
    @Override
    public CatalogSnapshot refresh(String reason) {
        log.debug("Catalog reset requested [{}] but caching is off — nothing to reset.", reason);
        return loader.load();
    }
}
