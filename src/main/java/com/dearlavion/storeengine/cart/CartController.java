package com.dearlavion.storeengine.cart;

import com.dearlavion.storeengine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService service;

    @GetMapping
    public CartView get(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.view(user.userId());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartView add(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody AddItemRequest dto) {
        return service.addItem(user.userId(), dto.productId(), dto.quantity());
    }

    @PutMapping("/items/{productId}")
    public CartView update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String productId,
                            @Valid @RequestBody UpdateItemRequest dto) {
        return service.updateItem(user.userId(), productId, dto.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartView remove(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String productId) {
        return service.removeItem(user.userId(), productId);
    }

    @DeleteMapping
    public CartView clear(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.clear(user.userId());
    }
}
