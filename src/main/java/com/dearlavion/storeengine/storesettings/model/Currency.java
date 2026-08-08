package com.dearlavion.storeengine.storesettings.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Currencies a user can pick as their preferred display/settlement currency (ISO 4217). USD is
 * the default/base; PHP is first-class since the store's payment methods settle there. */
public final class Currency {

    public static final List<String> SUPPORTED_CURRENCIES = List.of("USD", "PHP", "EUR", "GBP", "AUD", "SGD", "CAD", "JPY");

    /** Base currency that catalog prices are stored in — every rate is "units of X per 1 USD". */
    public static final String BASE_CURRENCY = "USD";

    /** Seed/fallback exchange rates (units per 1 USD). Admins override these in the dashboard;
     * they're only a sensible starting point so conversion works before anyone sets real rates. */
    public static final Map<String, Double> DEFAULT_RATES = new LinkedHashMap<>();

    static {
        DEFAULT_RATES.put("USD", 1.0);
        DEFAULT_RATES.put("PHP", 58.0);
        DEFAULT_RATES.put("EUR", 0.92);
        DEFAULT_RATES.put("GBP", 0.79);
        DEFAULT_RATES.put("AUD", 1.52);
        DEFAULT_RATES.put("SGD", 1.35);
        DEFAULT_RATES.put("CAD", 1.36);
        DEFAULT_RATES.put("JPY", 157.0);
    }

    private Currency() {
    }
}
