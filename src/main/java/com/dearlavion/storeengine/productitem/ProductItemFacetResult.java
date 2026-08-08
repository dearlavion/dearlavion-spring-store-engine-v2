package com.dearlavion.storeengine.productitem;

import java.util.List;

/** Maps the $facet stage's {content, totalCount} shape directly. */
public record ProductItemFacetResult(List<ProductItemView> content, List<CountHolder> totalCount) {
    public record CountHolder(long count) {
    }

    public long total() {
        return totalCount != null && !totalCount.isEmpty() ? totalCount.get(0).count() : 0;
    }
}
