package com.dearlavion.storeengine.orders;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A placed order — backs /profile/track-packages and /admin/orders. Delivery status is real and
 * admin-controlled (set via the admin/orders actions), not derived from elapsed time. */
@Getter
@Setter
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    @Indexed
    private String userId;

    /** Human-facing reference shown on the confirmation screen, e.g. TB-123456. */
    private String reference;

    private List<OrderItem> items = new ArrayList<>();

    /** Recipient details captured at checkout. Required on new orders; orders placed before this
     * field existed will simply have it null. */
    private Shipping shipping;

    private double total;

    /** Flat shipping fee actually charged (0 when the cart qualified for free shipping) — a
     * snapshot of the store's shippingFee setting at checkout time. */
    private double shippingFee = 0;

    private String currency = "USD";

    /** The amount + currency the customer was actually asked to pay at checkout (the catalog total
     * converted into their chosen currency). `total`/`currency` above remain the base (USD) snapshot. */
    private Double chargedAmount;

    private String chargedCurrency;

    /** Payment state, synced from the payment-service. UNPAID until the customer submits proof
     * (-> PENDING), then PAID/REJECTED once an employee reviews it. */
    @Indexed
    private String paymentStatus = "UNPAID";

    /** The payment-service Payment id that last set the status (audit link). */
    private String paymentId;

    /** Fulfillment state, set by admin actions on /admin/orders. Can only advance
     * Processing -&gt; Shipped -&gt; Delivered, and only once paymentStatus is PAID (enforced in
     * OrdersService, not schema validation). */
    @Indexed
    private String deliveryStatus = "Processing";

    private Instant shippedAt;

    private Instant deliveredAt;

    /** Admin-only visibility flag — pulls an order out of the default /admin/orders view into a
     * separate Archived view, without deleting it or touching fulfillment state. */
    @Indexed
    private boolean archived = false;

    private Instant archivedAt;

    /** Customer-initiated cancellation — self-service, only while deliveryStatus is still
     * 'Processing'. Once set, the order can no longer be shipped or have its inventory flagged
     * updated (enforced in OrdersService). */
    @Indexed
    private boolean cancelled = false;

    private Instant cancelledAt;

    private Instant placedAt;
}
