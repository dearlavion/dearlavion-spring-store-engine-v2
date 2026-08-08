package com.dearlavion.storeengine.storesettings;

import com.dearlavion.storeengine.storesettings.model.StoreSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StoreSettingsRepository extends MongoRepository<StoreSettings, String> {
}
