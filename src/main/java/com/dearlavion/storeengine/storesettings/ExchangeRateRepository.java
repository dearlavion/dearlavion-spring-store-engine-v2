package com.dearlavion.storeengine.storesettings;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExchangeRateRepository extends MongoRepository<ExchangeRate, String> {
}
