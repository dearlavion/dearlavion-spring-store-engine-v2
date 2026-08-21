package com.dearlavion.storeengine.catalog;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CatalogCacheSettingsRepository extends MongoRepository<CatalogCacheSettings, String> {
}
