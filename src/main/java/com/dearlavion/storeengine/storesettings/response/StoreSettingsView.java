package com.dearlavion.storeengine.storesettings.response;

import java.time.Instant;

public record StoreSettingsView(
        double freeShippingMinimum, double shippingFee, String defaultMediaProvider, int maxImageSizeKb,
        Instant updatedAt
) {
}
