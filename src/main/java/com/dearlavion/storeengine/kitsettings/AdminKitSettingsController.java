package com.dearlavion.storeengine.kitsettings;

import com.dearlavion.storeengine.kitsettings.model.KitSettings;
import com.dearlavion.storeengine.kitsettings.request.UpdateKitSettingsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/kit-settings")
@RequiredArgsConstructor
public class AdminKitSettingsController {

    private final KitSettingsService service;

    /** Un-suffixed means the survey, so an older client's saves still land where they used to. */
    @PutMapping
    public KitSettings update(@Valid @RequestBody UpdateKitSettingsRequest body) {
        return service.update(KitSettings.SURVEY_ID, body);
    }

    @PutMapping("/{context}")
    public KitSettings updateContext(@PathVariable String context, @Valid @RequestBody UpdateKitSettingsRequest body) {
        String id = switch (context) {
            case "survey" -> KitSettings.SURVEY_ID;
            case "product-form" -> KitSettings.PRODUCT_ID;
            default -> throw new IllegalArgumentException("Unknown kit settings context: " + context);
        };
        return service.update(id, body);
    }
}
