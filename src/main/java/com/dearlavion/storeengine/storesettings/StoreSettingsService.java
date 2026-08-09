package com.dearlavion.storeengine.storesettings;

import com.dearlavion.storeengine.storesettings.model.ImageUploadSettings;
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
    private final ImageUploadSettingsRepository imageUploadRepository;

    /** Current store settings (defaults applied when nothing is stored yet). Combines two separate
     * `store_settings` documents — shipping ({@link StoreSettings#SINGLETON_ID}) and image upload
     * ({@link ImageUploadSettings#SINGLETON_ID}) — into one view for the frontend. */
    public StoreSettingsView get() {
        StoreSettings shipping = repository.findById(StoreSettings.SINGLETON_ID).orElse(null);
        ImageUploadSettings imageUpload = imageUploadRepository.findById(ImageUploadSettings.SINGLETON_ID).orElse(null);
        Instant updatedAt = latest(shipping != null ? shipping.getUpdatedAt() : null,
                imageUpload != null ? imageUpload.getUpdatedAt() : null);
        return new StoreSettingsView(
                shipping != null ? shipping.getFreeShippingMinimum() : 0,
                shipping != null ? shipping.getShippingFee() : 0,
                imageUpload != null && imageUpload.getDefaultMediaProvider() != null ? imageUpload.getDefaultMediaProvider() : "s3",
                imageUpload != null && imageUpload.getMaxImageSizeKb() > 0 ? imageUpload.getMaxImageSizeKb() : 30,
                updatedAt
        );
    }

    /** Upsert the provided settings (admin). Only valid, defined fields are applied; shipping
     * fields and image-upload fields are written to their own documents. */
    public StoreSettingsView update(UpdateStoreSettingsRequest patch) {
        if (patch.freeShippingMinimum() != null || patch.shippingFee() != null) {
            StoreSettings doc = repository.findById(StoreSettings.SINGLETON_ID).orElseGet(StoreSettings::new);
            if (patch.freeShippingMinimum() != null && patch.freeShippingMinimum() >= 0) {
                doc.setFreeShippingMinimum(patch.freeShippingMinimum());
            }
            if (patch.shippingFee() != null && patch.shippingFee() >= 0) {
                doc.setShippingFee(patch.shippingFee());
            }
            doc.setUpdatedAt(Instant.now());
            repository.save(doc);
        }
        if (patch.defaultMediaProvider() != null || patch.maxImageSizeKb() != null) {
            ImageUploadSettings doc = imageUploadRepository.findById(ImageUploadSettings.SINGLETON_ID).orElseGet(ImageUploadSettings::new);
            if (patch.defaultMediaProvider() != null) {
                doc.setDefaultMediaProvider(patch.defaultMediaProvider());
            }
            if (patch.maxImageSizeKb() != null && patch.maxImageSizeKb() > 0) {
                doc.setMaxImageSizeKb(patch.maxImageSizeKb());
            }
            doc.setUpdatedAt(Instant.now());
            imageUploadRepository.save(doc);
        }
        return get();
    }

    private static Instant latest(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
