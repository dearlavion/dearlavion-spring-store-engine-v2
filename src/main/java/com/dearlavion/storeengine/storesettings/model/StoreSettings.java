package com.dearlavion.storeengine.storesettings.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** The free-shipping threshold, stored as one document in the `store_settings` collection keyed
 * by `_id: "free_shipping_minimum"`. Exchange rates live in a separate `_id: "exchange_rate"` doc
 * in the same collection (see {@link ExchangeRate}) —
 * each store-wide setting is its own entry. */
@Getter
@Setter
@Document(collection = "store_settings")
public class StoreSettings {

    public static final String SINGLETON_ID = "free_shipping_minimum";

    @Id
    private String id = SINGLETON_ID;

    /** Cart subtotal (base currency, USD) at/above which shipping is free. 0 = no threshold. */
    private double freeShippingMinimum = 0;

    /** Flat shipping fee (base currency, USD) charged when the cart is below freeShippingMinimum. */
    private double shippingFee = 0;

    private Instant updatedAt;
}
