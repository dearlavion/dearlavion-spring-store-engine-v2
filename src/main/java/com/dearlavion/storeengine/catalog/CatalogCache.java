package com.dearlavion.storeengine.catalog;

/**
 * Holds the current {@link CatalogSnapshot}. Deliberately an interface with only these two
 * operations, so the storage strategy can change without touching any caller.
 *
 * <p>The implementation today keeps the snapshot in this JVM's heap, which is correct while the
 * service runs as a single instance. Behind a load balancer that breaks: a write handled by one
 * instance leaves the others serving stale data indefinitely, with no error. The fix at that point
 * is a different implementation of this interface — one backed by a shared store, or one that polls
 * a version counter — and nothing else in the codebase needs to know.
 *
 * <p>Note for a future distributed implementation: {@link CatalogSnapshot} would need to be
 * serializable, and building it stays the job of {@link CatalogSnapshotLoader} rather than the
 * cache, so that logic is not duplicated per strategy.
 */
public interface CatalogCache {

    /** The current snapshot. Never null once the application has started. */
    CatalogSnapshot get();

    /**
     * Rebuild from the database and publish the result.
     *
     * @param reason what triggered this, logged verbatim, so the cron and the admin button don't
     *               produce identical log lines.
     */
    CatalogSnapshot refresh(String reason);

    /** For callers with nothing useful to say; prefer the overload above. */
    default CatalogSnapshot refresh() {
        return refresh("unspecified");
    }
}
