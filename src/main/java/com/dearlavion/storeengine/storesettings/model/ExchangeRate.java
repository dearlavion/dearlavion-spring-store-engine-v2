package com.dearlavion.storeengine.storesettings.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Admin-managed exchange rates, stored as one document in the `store_settings` collection keyed
 * by `_id: "exchange_rate"` (the free-shipping threshold is a separate
 * `_id: "free_shipping_minimum"` doc — see {@link StoreSettings} — in the same collection).
 * `rates` maps each supported currency to "units per 1 base" (base = USD), so a catalog price in
 * USD is converted by `price * rates[target]`. */
@Getter
@Setter
@Document(collection = "store_settings")
public class ExchangeRate {

    public static final String SINGLETON_ID = "exchange_rate";

    @Id
    private String id = SINGLETON_ID;

    private String base = Currency.BASE_CURRENCY;

    private Map<String, Double> rates = new LinkedHashMap<>();

    private Instant updatedAt;
}
