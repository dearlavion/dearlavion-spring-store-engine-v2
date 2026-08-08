package com.dearlavion.storeengine.cart;

import com.dearlavion.storeengine.cart.model.Cart;
import com.dearlavion.storeengine.cart.model.CartItem;
import com.dearlavion.storeengine.cart.response.CartView;
import com.dearlavion.storeengine.productitem.model.ProductItem;
import com.dearlavion.storeengine.productitem.ProductItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository repository;
    private final ProductItemService productItems;
    private final CartMapper mapper;

    private Cart getOrCreate(String userId) {
        return repository.findByUserId(userId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setUpdatedAt(Instant.now());
            return repository.save(cart);
        });
    }

    public CartView view(String userId) {
        return mapper.toView(userId, getOrCreate(userId));
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
