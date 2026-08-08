package com.dearlavion.storeengine.orders;

import com.dearlavion.storeengine.common.exception.ConflictException;
import com.dearlavion.storeengine.common.exception.NotFoundException;
import com.dearlavion.storeengine.orders.model.Order;
import com.dearlavion.storeengine.orders.model.OrderItem;
import com.dearlavion.storeengine.orders.model.Shipping;
import com.dearlavion.storeengine.productitem.model.ProductItem;
import com.dearlavion.storeengine.productitem.ProductItemService;
import com.dearlavion.storeengine.productitem.request.UpdateProductItemRequest;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdersService {

    private static final Logger log = LoggerFactory.getLogger(OrdersService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository repository;
    private final MongoTemplate mongoTemplate;
    private final ProductItemService productItems;

    /** Generate a TB-###### reference like the frontend confirmation screen. */
    private static String generateReference() {
        return "TB-" + (100000 + RANDOM.nextInt(900000));
    }

    public record PlaceOrderInput(
            List<OrderItem> items, Shipping shipping, double total, Double shippingFee,
            String currency, String reference, Double chargedAmount, String chargedCurrency
    ) {
    }

    public Order place(String userId, PlaceOrderInput input) {
        Order order = new Order();
        order.setUserId(userId);
        order.setReference(input.reference() != null && !input.reference().isBlank()
                ? input.reference().trim() : generateReference());
        order.setItems(input.items());
        order.setShipping(input.shipping());
        order.setTotal(input.total());
        order.setShippingFee(input.shippingFee() != null ? input.shippingFee() : 0);
        order.setCurrency(input.currency() != null ? input.currency() : "USD");
        order.setChargedAmount(input.chargedAmount());
        order.setChargedCurrency(input.chargedCurrency());
        order.setPlacedAt(Instant.now());
        return repository.save(order);
    }

    /** The caller's orders, newest first. */
    public List<Order> listForUser(String userId) {
        return repository.findByUserIdOrderByPlacedAtDesc(userId);
    }

    /** Match an order by its Mongo `_id` or its human `reference` (TB-######) — the frontend uses
     * the reference as the order's id throughout, so both need to resolve. */
    private Query idOrReferenceQuery(String idOrRef, String userId) {
        Criteria idCriteria = ObjectId.isValid(idOrRef) ? Criteria.where("_id").is(idOrRef) : Criteria.where("reference").is(idOrRef);
        Query query = Query.query(idCriteria);
        if (userId != null) query.addCriteria(Criteria.where("userId").is(userId));
        return query;
    }

    private Order requireByIdOrReference(String idOrRef, String userId) {
        Order order = mongoTemplate.findOne(idOrReferenceQuery(idOrRef, userId), Order.class);
        if (order == null) throw new NotFoundException("Order not found");
        return order;
    }

    public Order getForUser(String userId, String idOrRef) {
        return requireByIdOrReference(idOrRef, userId);
    }

    /** Self-service cancellation — only while still 'Processing' and only once money hasn't
     * actually moved: blocked while paymentStatus is PENDING (a submission is awaiting review) or
     * PAID (already approved — cancelling a paid order needs a refund, not self-service).
     * UNPAID/REJECTED orders can still be cancelled instantly. Ownership-scoped like getForUser. */
    public Order cancelOrder(String userId, String idOrRef) {
        Order order = requireByIdOrReference(idOrRef, userId);
        if (order.isCancelled()) throw new ConflictException("Order is already cancelled");
        if (!"Processing".equals(order.getDeliveryStatus())) {
            throw new ConflictException("Order can no longer be cancelled");
        }
        if ("PENDING".equals(order.getPaymentStatus())) {
            throw new ConflictException("Order cannot be cancelled while payment is being verified");
        }
        if ("PAID".equals(order.getPaymentStatus())) {
            throw new ConflictException("Order cannot be self-cancelled once payment is approved");
        }
        order.setCancelled(true);
        order.setCancelledAt(Instant.now());
        return repository.save(order);
    }

    /** Self-service: flip to PENDING right after the customer submits proof of payment. Never
     * downgrades an already-PAID order. Ownership-scoped like getForUser. */
    public Order markPaymentPending(String userId, String idOrRef, String paymentId) {
        Order order = requireByIdOrReference(idOrRef, userId);
        if ("PAID".equals(order.getPaymentStatus())) return order;
        order.setPaymentStatus("PENDING");
        if (paymentId != null) order.setPaymentId(paymentId);
        return repository.save(order);
    }

    /** Admin: set an order's payment status (called by the payment-service on review). */
    public Order setPaymentStatus(String idOrRef, String paymentStatus, String paymentId) {
        Order order = requireByIdOrReference(idOrRef, null);
        order.setPaymentStatus(paymentStatus);
        if (paymentId != null) order.setPaymentId(paymentId);
        return repository.save(order);
    }

    // ---- admin (fulfillment) ----

    /** Every order across every customer, newest first — backs /admin/orders. */
    public List<Order> listAllForAdmin(String paymentStatus, String deliveryStatus) {
        Query query = new Query();
        if (paymentStatus != null) query.addCriteria(Criteria.where("paymentStatus").is(paymentStatus));
        if (deliveryStatus != null) query.addCriteria(Criteria.where("deliveryStatus").is(deliveryStatus));
        query.with(Sort.by(Sort.Order.desc("placedAt")));
        return mongoTemplate.find(query, Order.class);
    }

    /** Any order, no ownership check (admin can open any customer's order). */
    public Order getForAdmin(String idOrRef) {
        return requireByIdOrReference(idOrRef, null);
    }

    /** Processing -> Shipped. Only once payment has actually been approved. Also decrements
     * catalog stock for every line that hasn't had it done yet — each line's stock write is
     * isolated: a failure there logs and leaves that line's inventoryUpdated false (so the manual
     * per-item endpoint still offers a retry) rather than blocking the ship itself. */
    public Order markShipped(String idOrRef) {
        Order order = requireByIdOrReference(idOrRef, null);
        if (order.isCancelled()) throw new ConflictException("Order was cancelled");
        if (!"PAID".equals(order.getPaymentStatus())) {
            throw new ConflictException("Order must be paid before it can be marked shipped");
        }
        if (!"Processing".equals(order.getDeliveryStatus())) {
            throw new ConflictException("Cannot mark shipped from status " + order.getDeliveryStatus());
        }

        for (OrderItem item : order.getItems()) {
            if (item.getProductItemId() == null || item.isInventoryUpdated()) continue;
            try {
                ProductItem productItem = productItems.getById(item.getProductItemId());
                int newStock = Math.max(0, productItem.getStock() - item.getQuantity());
                productItems.update(item.getProductItemId(), new UpdateProductItemRequest(
                        null, null, null, null, null, null, null, null, null, null,
                        newStock, null, null, null, null, null));
                item.setInventoryUpdated(true);
            } catch (Exception e) {
                log.warn("markShipped: inventory decrement failed for item {} on order {}: {}",
                        item.getProductItemId(), order.getReference(), e.getMessage());
            }
        }

        order.setDeliveryStatus("Shipped");
        order.setShippedAt(Instant.now());
        return repository.save(order);
    }

    /** Shipped -> Delivered. */
    public Order markDelivered(String idOrRef) {
        Order order = requireByIdOrReference(idOrRef, null);
        if (!"Shipped".equals(order.getDeliveryStatus())) {
            throw new ConflictException("Cannot mark delivered from status " + order.getDeliveryStatus());
        }
        order.setDeliveryStatus("Delivered");
        order.setDeliveredAt(Instant.now());
        return repository.save(order);
    }

    /** Pulls an order out of the default admin view into a separate Archived view. Purely a
     * visibility flag — independent of payment/delivery state. */
    public Order markArchived(String idOrRef) {
        Order order = requireByIdOrReference(idOrRef, null);
        order.setArchived(true);
        order.setArchivedAt(Instant.now());
        return repository.save(order);
    }

    public Order markUnarchived(String idOrRef) {
        Order order = requireByIdOrReference(idOrRef, null);
        order.setArchived(false);
        order.setArchivedAt(null);
        return repository.save(order);
    }

    /** Flags one line item as having had its catalog stock manually decremented — idempotency
     * marker only; markShipped() does this automatically for every line on ship, so this is the
     * fallback: pre-shipment manual correction, or per-item recovery if that decrement failed. */
    public Order markItemInventoryUpdated(String idOrRef, String productItemId) {
        Order order = requireByIdOrReference(idOrRef, null);
        if (order.isCancelled()) throw new ConflictException("Order was cancelled");
        if (!"PAID".equals(order.getPaymentStatus())) {
            throw new ConflictException("Order must be paid before updating inventory");
        }
        OrderItem item = order.getItems().stream()
                .filter(i -> productItemId.equals(i.getProductItemId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Order item not found"));
        item.setInventoryUpdated(true);
        return repository.save(order);
    }
}
