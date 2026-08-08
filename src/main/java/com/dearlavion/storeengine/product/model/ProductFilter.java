package com.dearlavion.storeengine.product.model;

import com.dearlavion.storeengine.product.request.ListProductsQuery;

/** Internal filter carrier — the public controller always leaves includeInactive false; the admin
 * controller sets it true. Mirrors product-query.ts's ProductFilter interface. */
public record ProductFilter(
        String destination,
        String season,
        String party,
        String category,
        String search,
        String sort,
        Integer page,
        Integer size,
        boolean includeInactive
) {
    public static ProductFilter fromQuery(ListProductsQuery q, boolean includeInactive) {
        return new ProductFilter(q.getDestination(), q.getSeason(), q.getParty(), q.getCategory(), q.getSearch(),
                q.getSort(), q.getPage(), q.getSize(), includeInactive);
    }
}
