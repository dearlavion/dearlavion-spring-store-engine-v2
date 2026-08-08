package com.dearlavion.storeengine.storesettings;

import com.dearlavion.storeengine.storesettings.model.ExchangeRate;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExchangeRateRepository extends MongoRepository<ExchangeRate, String> {
}
