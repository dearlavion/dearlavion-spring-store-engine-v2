package com.dearlavion.storeengine.shippingdetails;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ShippingDetailsRepository extends MongoRepository<ShippingDetails, String> {
    Optional<ShippingDetails> findByUserId(String userId);
}
