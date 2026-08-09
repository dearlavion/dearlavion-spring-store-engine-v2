package com.dearlavion.storeengine.storesettings;

import com.dearlavion.storeengine.storesettings.model.ImageUploadSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ImageUploadSettingsRepository extends MongoRepository<ImageUploadSettings, String> {
}
