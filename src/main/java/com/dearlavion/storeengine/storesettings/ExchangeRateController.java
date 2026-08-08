package com.dearlavion.storeengine.storesettings;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public: the storefront reads rates to convert catalog prices into the user's currency. */
@RestController
@RequestMapping("/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService service;

    @GetMapping
    public RatesView get() {
        return service.get();
    }
}
