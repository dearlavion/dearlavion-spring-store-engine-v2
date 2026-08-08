package com.dearlavion.storeengine.orders;

import com.dearlavion.storeengine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrdersService service;

    /** Record a placed order (called by checkout). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order place(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody PlaceOrderRequest dto) {
        List<OrderItem> items = dto.items().stream().map(i -> {
            OrderItem item = new OrderItem();
            item.setProductId(i.productId());
            item.setProductItemId(i.productItemId());
            item.setBrand(i.brand());
            item.setName(i.name());
            item.setIcon(i.icon() != null ? i.icon() : "");
            item.setQuantity(i.quantity());
            item.setPrice(i.price());
            item.setCurrency(i.currency() != null ? i.currency() : (dto.currency() != null ? dto.currency() : "USD"));
            item.setInventoryUpdated(false);
            return item;
        }).toList();

        Shipping shipping = null;
        if (dto.shipping() != null) {
            shipping = new Shipping();
            shipping.setFullName(dto.shipping().fullName());
            shipping.setEmail(dto.shipping().email());
            shipping.setAddress(dto.shipping().address());
            shipping.setCity(dto.shipping().city());
            shipping.setPostalCode(dto.shipping().postalCode());
        }

        return service.place(user.userId(), new OrdersService.PlaceOrderInput(
                items, shipping, dto.total(), dto.shippingFee(), dto.currency(), dto.reference(),
                dto.chargedAmount(), dto.chargedCurrency()
        ));
    }

    /** The caller's orders (newest first) — backs Track Packages. */
    @GetMapping
    public List<Order> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.listForUser(user.userId());
    }

    @GetMapping("/{id}")
    public Order get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        return service.getForUser(user.userId(), id);
    }

    /** Self-service cancellation — only while the order is still 'Processing'. */
    @PatchMapping("/{id}/cancel")
    public Order cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        return service.cancelOrder(user.userId(), id);
    }

    /** Self-service: flip payment status to PENDING right after the customer submits proof —
     * called by the payment-service, forwarding the customer's own token (their token can't pass
     * AdminGuard, which is why this lives here rather than on the admin-only endpoint). */
    @PatchMapping("/{id}/payment-pending")
    public Order markPaymentPending(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id,
                                     @RequestBody(required = false) MarkPaymentPendingRequest dto) {
        return service.markPaymentPending(user.userId(), id, dto != null ? dto.paymentId() : null);
    }
}
