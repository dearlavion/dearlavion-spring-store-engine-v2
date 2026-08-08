package com.dearlavion.storeengine.stats.response;

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
