package com.dearlavion.storeengine.kitsettings;

import com.dearlavion.storeengine.kitsettings.model.KitSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface KitSettingsRepository extends MongoRepository<KitSettings, String> {
}
