package com.dearlavion.storeengine.kitsettings;

import com.dearlavion.storeengine.kitsettings.model.KitSettings;
import com.dearlavion.storeengine.kitsettings.request.UpdateKitSettingsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/kit-settings")
@RequiredArgsConstructor
public class AdminKitSettingsController {

    private final KitSettingsService service;

    @PutMapping
    public KitSettings update(@Valid @RequestBody UpdateKitSettingsRequest body) {
        return service.update(body);
    }
}
