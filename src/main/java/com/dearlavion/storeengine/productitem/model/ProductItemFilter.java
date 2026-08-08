package com.dearlavion.storeengine.productitem.model;

import com.dearlavion.storeengine.productitem.request.ListProductItemsQuery;

public record ProductItemFilter(
        String id, // a single item's own id — set internally, not a public query param
        String productId,
        String destination,
        String season,
        String party,
        String category,
        String search,
        String sort,
        Integer page,
        Integer size
) {
    public static ProductItemFilter fromQuery(ListProductItemsQuery q) {
        return new ProductItemFilter(q.getId(), q.getProductId(), q.getDestination(), q.getSeason(),
                q.getParty(), q.getCategory(), q.getSearch(), q.getSort(), q.getPage(), q.getSize());
    }
}
