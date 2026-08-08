package com.dearlavion.storeengine.storesettings;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface StoreSettingsRepository extends MongoRepository<StoreSettings, String> {
}
