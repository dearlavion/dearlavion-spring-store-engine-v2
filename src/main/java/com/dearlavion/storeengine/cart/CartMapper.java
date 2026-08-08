package com.dearlavion.storeengine.cart;

import com.dearlavion.storeengine.cart.model.Cart;
import com.dearlavion.storeengine.cart.model.CartItem;
import com.dearlavion.storeengine.cart.response.CartItemView;
import com.dearlavion.storeengine.cart.response.CartView;
import com.dearlavion.storeengine.productitem.model.ProductItem;
import com.dearlavion.storeengine.productitem.ProductItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CartMapper {

    private final ProductItemService productItems;

    /** Cart enriched with product-item snapshots + a computed subtotal, for the UI. A cart line's
     * `productId` is a ProductItem id (the specific purchasable SKU/brand chosen), not a Product id. */
    public CartView toView(String userId, Cart cart) {
        List<CartItemView> items = cart.getItems().stream().map(this::toItemView).toList();
        double subtotal = items.stream().mapToDouble(CartItemView::lineTotal).sum();
        int itemCount = items.stream().mapToInt(CartItemView::quantity).sum();
        return new CartView(userId, items, subtotal, itemCount);
    }

    private CartItemView toItemView(CartItem i) {
        ProductItem product;
        try {
            product = productItems.getById(i.getProductId());
        } catch (Exception e) {
            product = null;
        }
        double lineTotal = (product != null ? product.getPrice() : 0) * i.getQuantity();
        return new CartItemView(i.getProductId(), i.getQuantity(), product, lineTotal);
    }
}
