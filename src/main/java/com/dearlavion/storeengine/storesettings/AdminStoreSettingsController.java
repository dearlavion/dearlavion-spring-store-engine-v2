package com.dearlavion.storeengine.storesettings;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin: edit store settings from the dashboard Settings page. */
@RestController
@RequestMapping("/admin/store-settings")
@RequiredArgsConstructor
public class AdminStoreSettingsController {

    private final StoreSettingsService service;

    @GetMapping
    public StoreSettingsView get() {
        return service.get();
    }

    @PutMapping
    public StoreSettingsView update(@Valid @RequestBody UpdateStoreSettingsRequest dto) {
        return service.update(dto);
    }
}
