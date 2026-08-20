package com.dearlavion.storeengine.catalog;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Keeps the snapshot in this JVM's heap — about 63 KB for the current catalog, so there is nothing
 * to evict and no key to look up. That is why this is a plain AtomicReference rather than Caffeine,
 * Spring Cache or Redis: those solve keying, eviction or sharing, and none of the three applies to a
 * single small value. Redis in particular would reintroduce the network round trip this exists to
 * remove.
 *
 * <p>See {@link CatalogCache} for what changes if the service ever runs as more than one instance.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.catalog-cache-enabled", havingValue = "true")
@RequiredArgsConstructor
public class InMemoryCatalogCache implements CatalogCache {

    private final CatalogSnapshotLoader loader;
    private final AtomicReference<CatalogSnapshot> snapshot = new AtomicReference<>();

    /**
     * Built before the application serves traffic, so no request races an empty snapshot and a
     * misconfigured database fails at boot rather than on a shopper's first survey.
     */
    @PostConstruct
    void loadOnStartup() {
        CatalogSnapshot loaded = loader.load();
        snapshot.set(loaded);
        log.info("Catalog snapshot loaded at startup: {} products, {} items.",
                loaded.productCount(), loaded.itemCount());
    }

    @Override
    public CatalogSnapshot get() {
        return snapshot.get();
    }

    /**
     * A failed rebuild keeps the previous snapshot rather than clearing it. A transient Atlas blip
     * should not take surveys down when perfectly good data is already in memory — stale beats
     * absent here, and the log plus the admin-visible builtAt make the staleness discoverable.
     */
    @Override
    public CatalogSnapshot refresh() {
        try {
            CatalogSnapshot loaded = loader.load();
            snapshot.set(loaded);
            log.info("Catalog snapshot refreshed: {} products, {} items.",
                    loaded.productCount(), loaded.itemCount());
            return loaded;
        } catch (RuntimeException e) {
            CatalogSnapshot current = snapshot.get();
            log.error("Catalog snapshot refresh failed; still serving the snapshot built at {}.",
                    current != null ? current.builtAt() : "(none)", e);
            if (current == null) throw e;
            return current;
        }
    }
}
