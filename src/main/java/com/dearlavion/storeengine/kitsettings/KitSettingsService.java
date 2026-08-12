package com.dearlavion.storeengine.kitsettings;

import com.dearlavion.storeengine.kitsettings.model.KitSettings;
import com.dearlavion.storeengine.kitsettings.model.SectionSettings;
import com.dearlavion.storeengine.kitsettings.request.UpdateKitSettingsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads and writes the single {@code kit_settings} document — the survey's question order and each
 * question's optional/required, single/multiple behaviour.
 *
 * <p>Lives here rather than in master-data-service because this service owns the survey itself
 * (KitEngine/SurveyService): master-data owns *what the options are*, this owns *how the survey
 * asks about them*.
 *
 * <p>Consequence of that split: the keys used here name collections owned by the other service, and
 * this one has no client for it, so keys are only checked for being non-blank. The admin UI picks
 * them from the live registry, which is the real enforcement point — the same argument
 * CreateProductRequest already makes for destinations/seasons/parties.
 */
@Service
@RequiredArgsConstructor
public class KitSettingsService {

    private final KitSettingsRepository repository;

    public KitSettings get() {
        return repository.findById(KitSettings.SINGLETON_ID).orElseGet(KitSettings::new);
    }

    public KitSettings update(UpdateKitSettingsRequest patch) {
        KitSettings settings = get();

        if (patch.order() != null) {
            for (String key : patch.order()) {
                if (key == null || key.isBlank()) {
                    throw new IllegalArgumentException("Collection keys must not be blank");
                }
            }
            settings.setOrder(patch.order());
        }

        if (patch.sections() != null) {
            // Merge rather than replace, so a client saving one section's behaviour can't silently
            // drop the rest.
            Map<String, SectionSettings> merged = new LinkedHashMap<>(settings.getSections());
            for (Map.Entry<String, SectionSettings> entry : patch.sections().entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    throw new IllegalArgumentException("Collection keys must not be blank");
                }
                merged.put(entry.getKey(), entry.getValue());
            }
            settings.setSections(merged);
        }

        settings.setId(KitSettings.SINGLETON_ID);
        return repository.save(settings);
    }
}
