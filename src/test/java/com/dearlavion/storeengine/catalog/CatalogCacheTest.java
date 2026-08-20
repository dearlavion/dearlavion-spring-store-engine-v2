package com.dearlavion.storeengine.catalog;

import com.dearlavion.storeengine.survey.KitEngine;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The cache's own behaviour, without Spring or a database — these run on every build, unlike the
 * Testcontainers integration tests which need Docker.
 */
class CatalogCacheTest {

    private static CatalogSnapshot snapshotAt(Instant at) {
        return new CatalogSnapshot(List.of(), Map.of(),
                new KitEngine.AxisDomains(Set.of(), Set.of(), Set.of()), Map.of(), at);
    }

    @Test
    void refreshPublishesTheNewSnapshot() {
        Instant first = Instant.parse("2020-01-01T00:00:00Z");
        Instant second = Instant.parse("2020-01-02T00:00:00Z");
        CatalogSnapshotLoader loader = Mockito.mock(CatalogSnapshotLoader.class);
        Mockito.when(loader.load()).thenReturn(snapshotAt(first), snapshotAt(second));

        InMemoryCatalogCache cache = new InMemoryCatalogCache(loader);
        cache.loadOnStartup();
        assertThat(cache.get().builtAt()).isEqualTo(first);

        cache.refresh();
        assertThat(cache.get().builtAt()).isEqualTo(second);
    }

    @Test
    void aFailedRefreshKeepsServingThePreviousSnapshot() {
        // A transient Atlas blip must not take surveys down when good data is already in memory.
        Instant good = Instant.parse("2020-01-01T00:00:00Z");
        CatalogSnapshotLoader loader = Mockito.mock(CatalogSnapshotLoader.class);
        Mockito.when(loader.load())
                .thenReturn(snapshotAt(good))
                .thenThrow(new IllegalStateException("mongo unavailable"));

        InMemoryCatalogCache cache = new InMemoryCatalogCache(loader);
        cache.loadOnStartup();
        CatalogSnapshot afterFailure = cache.refresh();

        assertThat(afterFailure.builtAt()).isEqualTo(good);
        assertThat(cache.get().builtAt()).isEqualTo(good);
    }

    @Test
    void aFailureBeforeAnySnapshotExistsPropagates() {
        // Nothing to fall back to, so failing loudly at startup beats serving an empty catalog.
        CatalogSnapshotLoader loader = Mockito.mock(CatalogSnapshotLoader.class);
        Mockito.when(loader.load()).thenThrow(new IllegalStateException("mongo unavailable"));

        InMemoryCatalogCache cache = new InMemoryCatalogCache(loader);

        assertThatThrownBy(cache::refresh).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void passThroughModeAlwaysReadsFresh() {
        CatalogSnapshotLoader loader = Mockito.mock(CatalogSnapshotLoader.class);
        Mockito.when(loader.load()).thenReturn(snapshotAt(Instant.now()));

        PassThroughCatalogCache cache = new PassThroughCatalogCache(loader);
        cache.get();
        cache.get();
        cache.refresh();

        Mockito.verify(loader, Mockito.times(3)).load();
    }
}
