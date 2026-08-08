package com.dearlavion.storeengine.cart;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** One line in a cart. `productId` is a ProductItem id (the specific purchasable SKU/brand
 * chosen), not a Product id. */
@Getter
@Setter
public class CartItem {
    private String productId;
    private int quantity;
    private Instant addedAt;
}
