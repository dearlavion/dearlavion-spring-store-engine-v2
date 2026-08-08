package com.dearlavion.storeengine.seed;

import java.util.List;

/** Maps one entry of seed/seed-data.json — mirrors seed-data.json's shape 1:1. price/currency get
 * split off onto the default ProductItem; everything else lands on Product (see seed.ts's
 * destructuring of `{ price, currency, image, stock, soldOut, ...productFields }`). */
public record SeedProductEntry(
        String name,
        String category,
        Double price,
        String currency,
        String description,
        String icon,
        Boolean popular,
        Boolean tested,
        List<String> destinations,
        List<String> seasons,
        List<String> parties
) {
}
