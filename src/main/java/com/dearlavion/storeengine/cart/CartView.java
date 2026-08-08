package com.dearlavion.storeengine.cart;

import java.util.List;

public record CartView(String userId, List<CartItemView> items, double subtotal, int itemCount) {
}
