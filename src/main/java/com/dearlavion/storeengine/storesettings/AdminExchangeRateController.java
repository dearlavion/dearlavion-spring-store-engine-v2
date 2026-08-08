package com.dearlavion.storeengine.storesettings;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Admin: set the exchange rates from the dashboard Settings page. */
@RestController
@RequestMapping("/admin/exchange-rates")
@RequiredArgsConstructor
public class AdminExchangeRateController {

    private final ExchangeRateService service;

    @GetMapping
    public RatesView get() {
        return service.get();
    }

    @PutMapping
    public RatesView update(@RequestBody UpdateRatesRequest dto) {
        return service.update(dto.rates() != null ? dto.rates() : Map.of());
    }
}
