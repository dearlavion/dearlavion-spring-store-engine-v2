package com.dearlavion.storeengine.storesettings;

import com.dearlavion.storeengine.storesettings.model.StoreSettings;
import com.dearlavion.storeengine.storesettings.request.UpdateStoreSettingsRequest;
import com.dearlavion.storeengine.storesettings.response.StoreSettingsView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class StoreSettingsService {

    private final StoreSettingsRepository repository;

    /** Current store settings (defaults applied when nothing is stored yet). */
    public StoreSettingsView get() {
        StoreSettings doc = repository.findById(StoreSettings.SINGLETON_ID).orElse(null);
        return new StoreSettingsView(
                doc != null ? doc.getFreeShippingMinimum() : 0,
                doc != null ? doc.getShippingFee() : 0,
                doc != null ? doc.getUpdatedAt() : null
        );
    }

    /** Upsert the provided settings (admin). Only valid, defined fields are applied. */
    public StoreSettingsView update(UpdateStoreSettingsRequest patch) {
        StoreSettings doc = repository.findById(StoreSettings.SINGLETON_ID).orElseGet(StoreSettings::new);
        if (patch.freeShippingMinimum() != null && patch.freeShippingMinimum() >= 0) {
            doc.setFreeShippingMinimum(patch.freeShippingMinimum());
        }
        if (patch.shippingFee() != null && patch.shippingFee() >= 0) {
            doc.setShippingFee(patch.shippingFee());
        }
        doc.setUpdatedAt(Instant.now());
        repository.save(doc);
        return get();
    }
}
