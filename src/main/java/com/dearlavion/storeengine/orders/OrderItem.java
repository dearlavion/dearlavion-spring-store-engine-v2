package com.dearlavion.storeengine.orders;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItem {
    /** The generic Product (slug) this line is for. */
    private String productId;
    /** The specific ProductItem (SKU/variant) actually purchased, and its brand — snapshot so the
     * exact variant is known even if the catalog changes later. */
    private String productItemId;
    private String brand;
    private String name;
    private String icon = "";
    private int quantity;
    private double price;
    private String currency = "USD";
    /** Set once admin has decremented catalog stock for this line — prevents double-decrementing
     * on a repeat click/reload. Purely a UI-idempotency flag; doesn't itself touch stock. */
    private boolean inventoryUpdated = false;
}
