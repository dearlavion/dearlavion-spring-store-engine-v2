package com.dearlavion.storeengine.storesettings;

import com.dearlavion.storeengine.storesettings.response.StoreSettingsView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public: the storefront reads store settings (e.g. free-shipping threshold) for cart/checkout. */
@RestController
@RequestMapping("/store-settings")
@RequiredArgsConstructor
public class StoreSettingsController {

    private final StoreSettingsService service;

    @GetMapping
    public StoreSettingsView get() {
        return service.get();
    }
}
