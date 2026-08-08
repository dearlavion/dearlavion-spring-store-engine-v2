package com.dearlavion.storeengine.cart;

import com.dearlavion.storeengine.productitem.ProductItem;

/** One cart line enriched with its product-item snapshot, for the UI. `product` is the raw
 * ProductItem entity (not the aggregated catalog view) — matches NestJS's CartService, which
 * calls productItems.getById() (a plain findById), not the public listing pipeline. */
public record CartItemView(String productId, int quantity, ProductItem product, double lineTotal) {
}
