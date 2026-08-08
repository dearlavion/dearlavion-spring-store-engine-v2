package com.dearlavion.storeengine.orders;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin: browse every order and drive fulfillment (mark shipped/delivered, flag inventory
 * updated) — backs /admin/orders. Payment-status updates are called by the payment-service, not
 * from this admin UI directly: PENDING when a customer submits proof, PAID/REJECTED on review. */
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrdersController {

    private final OrdersService service;

    @GetMapping
    public List<Order> list(@Valid AdminOrderFiltersQuery filters) {
        return service.listAllForAdmin(filters.getPaymentStatus(), filters.getDeliveryStatus());
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable String id) {
        return service.getForAdmin(id);
    }

    @PatchMapping("/{id}/payment-status")
    public Order updatePaymentStatus(@PathVariable String id, @Valid @RequestBody UpdatePaymentStatusRequest dto) {
        return service.setPaymentStatus(id, dto.status(), dto.paymentId());
    }

    @PatchMapping("/{id}/shipped")
    public Order markShipped(@PathVariable String id) {
        return service.markShipped(id);
    }

    @PatchMapping("/{id}/delivered")
    public Order markDelivered(@PathVariable String id) {
        return service.markDelivered(id);
    }

    @PatchMapping("/{id}/items/{productItemId}/inventory-updated")
    public Order markItemInventoryUpdated(@PathVariable String id, @PathVariable String productItemId) {
        return service.markItemInventoryUpdated(id, productItemId);
    }

    @PatchMapping("/{id}/archive")
    public Order markArchived(@PathVariable String id) {
        return service.markArchived(id);
    }

    @PatchMapping("/{id}/unarchive")
    public Order markUnarchived(@PathVariable String id) {
        return service.markUnarchived(id);
    }
}
