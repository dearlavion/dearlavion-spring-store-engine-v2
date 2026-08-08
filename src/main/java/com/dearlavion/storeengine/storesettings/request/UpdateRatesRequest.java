package com.dearlavion.storeengine.storesettings.request;

import java.util.Map;

/** Loose currency->rate map; the service keeps only known currencies with positive values. */
public record UpdateRatesRequest(Map<String, Double> rates) {
}
