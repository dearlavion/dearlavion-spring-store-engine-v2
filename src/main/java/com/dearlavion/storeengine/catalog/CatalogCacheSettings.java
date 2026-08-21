package com.dearlavion.storeengine.catalog;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * When the catalog snapshot should rebuild on a timer, stored in the `store_settings` collection
 * under its own `_id` — the convention every store-wide setting here follows (see
 * ImageUploadSettings, StoreSettings, ExchangeRate).
 *
 * <p>The write hooks on the seven product/product-item methods already keep the snapshot current
 * for anything the admin does through the app. This exists for what those hooks cannot see: a
 * migration, an edit made straight in Atlas, the seeder — and as a safety net if a future write path
 * is added without a hook.
 */
@Getter
@Setter
@Document(collection = "store_settings")
public class CatalogCacheSettings {

    public static final String SINGLETON_ID = "catalog_cache_settings";

    @Id
    private String id = SINGLETON_ID;

    /**
     * Spring cron expression, or blank for no scheduled refresh.
     *
     * <p>With no schedule and no manual reset, a snapshot lives until the process restarts. If
     * nobody is going to remember the button, set a cron.
     *
     * <p><strong>Six fields, not five.</strong> Spring's cron starts with seconds, so hourly is
     * {@code 0 0 * * * *}, not the Unix {@code 0 * * * *}. Validated with CronExpression.parse()
     * before it is stored, so a typo is rejected at save time rather than silently never firing.
     */
    private String refreshCron = "";

    private Instant updatedAt;
}
