package com.dearlavion.storeengine.productitem;

import java.util.List;

/** Matches the $project stage's exact output shape in ProductItemQueryBuilder — the public
 * catalog listing's flattened item+product view, distinct from the raw ProductItem entity. */
public record ProductItemView(
        String id,
        String productId,
        String name,
        String brand,
        String category,
        String description,
        double price,
        Double originalPrice,
        String currency,
        String image,
        List<String> images,
        List<ProductVideo> videos,
        String icon,
        int stock,
        boolean soldOut,
        boolean popular,
        boolean tested,
        List<String> destinations,
        List<String> seasons,
        List<String> parties
) {
}
