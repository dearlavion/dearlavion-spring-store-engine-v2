package com.dearlavion.storeengine.kitsettings;

import com.dearlavion.storeengine.kitsettings.model.KitSettings;
import java.util.List;
import java.util.Map;
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

    /**
     * Returns both contexts — and repeats the survey's own order/sections at the top level, so a
     * client written before the split keeps working unchanged and the two repos can still deploy
     * independently.
     */
    @GetMapping
    public Map<String, Object> get() {
        KitSettings survey = service.survey();
        KitSettings productForm = service.productForm();
        return Map.of(
                "order", survey.getOrder(),
                "sections", survey.getSections(),
                "survey", survey,
                "productForm", productForm);
    }
}
