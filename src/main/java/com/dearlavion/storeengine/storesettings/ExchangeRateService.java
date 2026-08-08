package com.dearlavion.storeengine.storesettings;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository repository;

    /** Current rates, with every supported currency present (stored value, else a sensible
     * default). Base currency is always 1. */
    public RatesView get() {
        ExchangeRate doc = repository.findById(ExchangeRate.SINGLETON_ID).orElse(null);
        Map<String, Double> stored = doc != null ? doc.getRates() : Map.of();
        Map<String, Double> rates = new LinkedHashMap<>();
        for (String code : Currency.SUPPORTED_CURRENCIES) {
            if (code.equals(Currency.BASE_CURRENCY)) {
                rates.put(code, 1.0);
            } else {
                Double value = stored.get(code);
                rates.put(code, value != null ? value : Currency.DEFAULT_RATES.getOrDefault(code, 1.0));
            }
        }
        return new RatesView(Currency.BASE_CURRENCY, rates, doc != null ? doc.getUpdatedAt() : null);
    }

    /** Upsert rates (admin). Only known currencies with positive numbers are kept; base stays 1. */
    public RatesView update(Map<String, Double> input) {
        Map<String, Double> rates = new LinkedHashMap<>();
        for (String code : Currency.SUPPORTED_CURRENCIES) {
            if (code.equals(Currency.BASE_CURRENCY)) continue;
            Double value = input != null ? input.get(code) : null;
            if (value != null && value > 0) rates.put(code, value);
        }
        ExchangeRate doc = repository.findById(ExchangeRate.SINGLETON_ID).orElseGet(ExchangeRate::new);
        doc.setBase(Currency.BASE_CURRENCY);
        doc.setRates(rates);
        doc.setUpdatedAt(Instant.now());
        repository.save(doc);
        return get();
    }
}
