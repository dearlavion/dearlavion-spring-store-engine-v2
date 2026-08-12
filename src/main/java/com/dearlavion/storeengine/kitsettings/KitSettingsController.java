package com.dearlavion.storeengine.kitsettings;

import com.dearlavion.storeengine.kitsettings.model.KitSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public read — the storefront survey needs the order and per-question behaviour without a token. */
@RestController
@RequestMapping("/kit-settings")
@RequiredArgsConstructor
public class KitSettingsController {

    private final KitSettingsService service;

    @GetMapping
    public KitSettings get() {
        return service.get();
    }
}
