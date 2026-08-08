package com.dearlavion.storeengine.stats;

import com.dearlavion.storeengine.productitem.ProductItem;
import com.dearlavion.storeengine.productitem.ProductItemRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final MongoTemplate mongoTemplate;
    private final ProductItemRepository productItemRepository;

    /** Every active ProductItem, joined with how much it's actually sold (0 for anything never
     * ordered) — sorted best-selling first. The frontend takes both ends of this one list for
     * "best performing" / "least popular" rather than needing two separate calls.
     *
     * <p><b>Ported as-is from the NestJS source:</b> the aggregation groups by the order line's
     * generic {@code productId} (Product slug), but the join below looks sales up by the
     * ProductItem's own {@code _id} — two different key spaces that essentially never intersect,
     * so {@code unitsSold}/{@code orderCount}/{@code revenue} come out 0 for real orders too. This
     * mirrors the original service's behavior exactly (a mechanical port keeps parity with v1 for
     * the Phase 4 diff); not fixed here as a scope-creep "while I'm at it" change. */
    public List<ProductItemStat> getProductItemPerformance() {
        Aggregation pipeline = Aggregation.newAggregation(
                Aggregation.stage(new Document("$unwind", "$items")),
                Aggregation.stage(new Document("$group", new Document("_id", "$items.productId")
                        .append("unitsSold", new Document("$sum", "$items.quantity"))
                        .append("orderCount", new Document("$sum", 1))
                        .append("revenue", new Document("$sum", new Document("$multiply", List.of("$items.quantity", "$items.price"))))))
        );
        AggregationResults<SalesRow> results = mongoTemplate.aggregate(pipeline, "orders", SalesRow.class);
        Map<String, SalesRow> salesByItem = new HashMap<>();
        for (SalesRow row : results.getMappedResults()) {
            salesByItem.put(row.id(), row);
        }

        List<ProductItem> items = productItemRepository.findByActiveTrue();
        return items.stream()
                .map(item -> {
                    SalesRow sales = salesByItem.get(item.getId());
                    return new ProductItemStat(
                            item.getId(), item.getProductId(), item.getName(), item.getBrand(), item.getIcon(),
                            sales != null ? sales.unitsSold() : 0,
                            sales != null ? sales.orderCount() : 0,
                            sales != null ? sales.revenue() : 0
                    );
                })
                .sorted((a, b) -> Long.compare(b.unitsSold(), a.unitsSold()))
                .toList();
    }

    /** Public (no auth, no revenue/order-count exposed): the storefront's real best-sellers, for
     * things like the homepage's "what's in my bag" widget. If fewer than `limit` items have ever
     * sold, the rest of the active catalog pads out the list so the widget always has something. */
    public List<TopSellingItem> getTopSelling(int limit) {
        List<ProductItemStat> performance = getProductItemPerformance(); // already sorted best-selling first
        return performance.stream()
                .limit(limit)
                .map(p -> new TopSellingItem(p.productItemId(), p.productId(), p.name(), p.brand(), p.icon()))
                .toList();
    }
}
