package com.dearlavion.storeengine.catalog;

import com.dearlavion.storeengine.product.model.Product;
import com.dearlavion.storeengine.productitem.model.ProductItem;
import com.dearlavion.storeengine.survey.KitEngine;
import com.dearlavion.storeengine.survey.model.EngineProduct;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The whole active catalog, shaped for the survey engine and built once instead of per request.
 *
 * <p>Immutable by contract: every collection here is unmodifiable, so the reference can be shared
 * across request threads without locking. A refresh builds a whole new instance and swaps it — a
 * reader either sees the old snapshot or the new one, never a half-built one.
 *
 * @param products      what the engine scores
 * @param byId          full documents, for hydrating the response
 * @param domains       axis value sets, which must be derived from the <em>whole</em> catalog —
 *                      computing them from a subset silently changes scores
 * @param itemsByProduct items per product, <strong>kept cheapest-first</strong>: SurveyService's
 *                      pickSizedItem falls back to {@code items.get(0)} for unsized products, so
 *                      the ordering is load-bearing, not cosmetic
 * @param builtAt       when this was loaded — surfaced to admins so a stale cache is visible
 */
public record CatalogSnapshot(
        List<EngineProduct> products,
        Map<String, Product> byId,
        KitEngine.AxisDomains domains,
        Map<String, List<ProductItem>> itemsByProduct,
        Instant builtAt
) {
    public int productCount() {
        return products.size();
    }

    public int itemCount() {
        return itemsByProduct.values().stream().mapToInt(List::size).sum();
    }
}
