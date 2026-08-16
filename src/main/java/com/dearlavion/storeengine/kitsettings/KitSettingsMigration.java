package com.dearlavion.storeengine.kitsettings;

import com.dearlavion.storeengine.kitsettings.model.KitSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds the two kit-settings documents: the survey's questions and the admin product form's
 * fields, split apart because they answer different questions about the same collection. A shopper
 * picks one trip length; a product suits several — so one shared `multiple` flag can't serve both.
 *
 * <p>Runs on every startup and does nothing once both exist. It seeds rather than leaving it to
 * {@link KitSettingsService#get}'s missing-document fallback on purpose: that fallback returns
 * defaults silently, so a half-applied migration would revert an admin's saved order with no error
 * anywhere. Seeding makes the state explicit and logs what it did.
 *
 * <p>The pre-split {@code kit_settings} document is copied, not moved — it stays as the rollback
 * path for one release, and is dropped deliberately later rather than as a side effect of an
 * upgrade.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class KitSettingsMigration implements ApplicationRunner {

    private final KitSettingsRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.findById(KitSettings.SURVEY_ID).isEmpty()) {
            KitSettings survey = repository.findById(KitSettings.LEGACY_ID).orElseGet(KitSettings::new);
            survey.setId(KitSettings.SURVEY_ID);
            repository.save(survey);
            log.info("Seeded {} ({}).", KitSettings.SURVEY_ID,
                    survey.getOrder().isEmpty() ? "defaults" : "carried over from " + KitSettings.LEGACY_ID);
        }

        if (repository.findById(KitSettings.PRODUCT_ID).isEmpty()) {
            KitSettings productForm = new KitSettings();
            productForm.setId(KitSettings.PRODUCT_ID);
            // Seeded from the product model's own shape, never copied from the survey — see
            // KitSettings.defaultProductSections().
            productForm.setOrder(KitSettings.DEFAULT_PRODUCT_ORDER);
            productForm.setSections(KitSettings.defaultProductSections());
            repository.save(productForm);
            log.info("Seeded {} from the product model's field shapes.", KitSettings.PRODUCT_ID);
        }
    }
}
