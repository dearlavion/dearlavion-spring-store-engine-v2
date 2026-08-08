package com.dearlavion.storeengine.cart;

import com.dearlavion.storeengine.productitem.ProductItem;
import com.dearlavion.storeengine.productitem.ProductItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository repository;
    private final ProductItemService productItems;

    private Cart getOrCreate(String userId) {
        return repository.findByUserId(userId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setUpdatedAt(Instant.now());
            return repository.save(cart);
        });
    }

    /** Cart enriched with product-item snapshots + a computed subtotal, for the UI. A cart line's
     * `productId` is a ProductItem id (the specific purchasable SKU/brand chosen), not a Product id. */
    public CartView view(String userId) {
        Cart cart = getOrCreate(userId);
        List<CartItemView> items = cart.getItems().stream().map(i -> {
            ProductItem product;
            try {
                product = productItems.getById(i.getProductId());
            } catch (Exception e) {
                product = null;
            }
            double lineTotal = (product != null ? product.getPrice() : 0) * i.getQuantity();
            return new CartItemView(i.getProductId(), i.getQuantity(), product, lineTotal);
        }).toList();
        double subtotal = items.stream().mapToDouble(CartItemView::lineTotal).sum();
        int itemCount = items.stream().mapToInt(CartItemView::quantity).sum();
        return new CartView(userId, items, subtotal, itemCount);
    }

    /** Add (or increment) an item. Validates the product item exists. */
    public CartView addItem(String userId, String productId, int quantity) {
        ProductItem item = productItems.getById(productId); // 404 if missing
        String pid = item.getId();
        Cart cart = getOrCreate(userId);
        Optional<CartItem> existing = cart.getItems().stream().filter(i -> i.getProductId().equals(pid)).findFirst();
        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + quantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setProductId(pid);
            newItem.setQuantity(quantity);
            newItem.setAddedAt(Instant.now());
            cart.getItems().add(newItem);
        }
        cart.setUpdatedAt(Instant.now());
        repository.save(cart);
        return view(userId);
    }

    /** Set an item's quantity; quantity <= 0 removes it. */
    public CartView updateItem(String userId, String productId, int quantity) {
        Cart cart = getOrCreate(userId);
        boolean exists = cart.getItems().stream().anyMatch(i -> i.getProductId().equals(productId));
        if (exists) {
            if (quantity <= 0) {
                cart.setItems(cart.getItems().stream()
                        .filter(i -> !i.getProductId().equals(productId))
                        .collect(Collectors.toCollection(ArrayList::new)));
            } else {
                cart.getItems().stream()
                        .filter(i -> i.getProductId().equals(productId))
                        .forEach(i -> i.setQuantity(quantity));
            }
            cart.setUpdatedAt(Instant.now());
            repository.save(cart);
        }
        return view(userId);
    }

    public CartView removeItem(String userId, String productId) {
        Cart cart = getOrCreate(userId);
        cart.setItems(cart.getItems().stream()
                .filter(i -> !i.getProductId().equals(productId))
                .collect(Collectors.toCollection(ArrayList::new)));
        cart.setUpdatedAt(Instant.now());
        repository.save(cart);
        return view(userId);
    }

    public CartView clear(String userId) {
        Cart cart = getOrCreate(userId);
        cart.setItems(new ArrayList<>());
        cart.setUpdatedAt(Instant.now());
        repository.save(cart);
        return view(userId);
    }
}
