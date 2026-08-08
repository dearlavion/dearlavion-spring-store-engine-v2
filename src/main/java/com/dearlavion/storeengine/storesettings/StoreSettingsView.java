package com.dearlavion.storeengine.storesettings;

import java.time.Instant;

public record StoreSettingsView(double freeShippingMinimum, double shippingFee, Instant updatedAt) {
}
