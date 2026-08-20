package com.dearlavion.storeengine.catalog;

import com.dearlavion.storeengine.product.model.Product;
import com.dearlavion.storeengine.productitem.ProductItemRepository;
import com.dearlavion.storeengine.productitem.model.ProductItem;
import com.dearlavion.storeengine.survey.KitEngine;
import com.dearlavion.storeengine.survey.model.EngineProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds a {@link CatalogSnapshot} from the database. Separate from {@link CatalogCache} so the
 * "how do we read the catalog" logic lives in one place regardless of how the snapshot is stored.
 *
 * <p>Reads through the repositories and MongoTemplate directly rather than through ProductService /
 * ProductItemService. Those services will call the cache to invalidate it, so depending on them here
 * would close a bean cycle — the same problem ProductService already works around with
 * ObjectProvider for ProductItemService. Keeping this dependency one-way avoids needing that.
 */
@Component
@RequiredArgsConstructor
public class CatalogSnapshotLoader {

    private final MongoTemplate mongoTemplate;
    private final ProductItemRepository productItems;

    public CatalogSnapshot load() {
        // Same query ProductService.allActive() ran per request.
        List<Product> active = mongoTemplate.find(Query.query(Criteria.where("active").is(true)), Product.class);

        List<EngineProduct> engineProducts = active.stream()
                .map(CatalogSnapshotLoader::toEngineProduct)
                .toList();

        Map<String, Product> byId = active.stream()
                .collect(Collectors.toUnmodifiableMap(Product::getId, Function.identity(), (a, b) -> a));

        return new CatalogSnapshot(
                engineProducts,
                byId,
                KitEngine.AxisDomains.of(engineProducts),
                loadItemsByProduct(),
                Instant.now());
    }

    /**
     * Cheapest-first within each product, matching the {@code Sort.by(asc("price"))} the per-request
     * query used. SurveyService.pickSizedItem falls back to {@code items.get(0)} when no item is
     * sized, so losing this ordering would quietly change which SKU a kit recommends.
     */
    private Map<String, List<ProductItem>> loadItemsByProduct() {
        Map<String, List<ProductItem>> byProduct = new LinkedHashMap<>();
        productItems.findByActiveTrue().stream()
                .sorted(Comparator.comparingDouble(ProductItem::getPrice))
                .forEach(item -> byProduct.computeIfAbsent(item.getProductId(), k -> new ArrayList<>()).add(item));
        byProduct.replaceAll((k, v) -> List.copyOf(v));
        return Map.copyOf(byProduct);
    }

    /** Moved from SurveyService — mapping a Product to what the engine needs is a catalog concern. */
    private static EngineProduct toEngineProduct(Product p) {
        return new EngineProduct(
                p.getId(),
                p.getName(),
                p.getCategory(),
                p.getDestinations() != null ? p.getDestinations() : List.of(),
                p.getSeasons() != null ? p.getSeasons() : List.of(),
                p.getParties() != null ? p.getParties() : List.of(),
                p.getActivities() != null ? p.getActivities() : List.of(),
                p.getTransportModes() != null ? p.getTransportModes() : List.of(),
                p.getDurations() != null ? p.getDurations() : List.of(),
                p.getGenders() != null ? p.getGenders() : List.of(),
                p.getKitCategories() != null ? p.getKitCategories() : List.of(),
                p.isPopular(),
                p.isTested()
        );
    }
}
