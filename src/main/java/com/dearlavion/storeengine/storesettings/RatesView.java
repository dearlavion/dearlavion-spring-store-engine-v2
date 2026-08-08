package com.dearlavion.storeengine.storesettings;

import java.time.Instant;
import java.util.Map;

public record RatesView(String base, Map<String, Double> rates, Instant updatedAt) {
}
