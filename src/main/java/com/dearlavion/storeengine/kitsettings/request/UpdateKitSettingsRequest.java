package com.dearlavion.storeengine.kitsettings.request;

import com.dearlavion.storeengine.kitsettings.model.SectionSettings;

import java.util.List;
import java.util.Map;

/**
 * Both fields are optional so a client can save just the order, just the per-section behaviour, or
 * both — a null field leaves what's stored untouched.
 *
 * @param sections keyed by collection key; only the keys present are replaced.
 */
public record UpdateKitSettingsRequest(List<String> order, Map<String, SectionSettings> sections) {
}
