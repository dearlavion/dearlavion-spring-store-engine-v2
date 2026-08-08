package com.dearlavion.storeengine.stats;

public record ProductItemStat(
        String productItemId,
        String productId,
        String name,
        String brand,
        String icon,
        long unitsSold,
        long orderCount,
        double revenue
) {
}
