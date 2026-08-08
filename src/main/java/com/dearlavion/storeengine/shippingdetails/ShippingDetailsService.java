package com.dearlavion.storeengine.shippingdetails;

import com.dearlavion.storeengine.shippingdetails.model.ShippingDetails;
import com.dearlavion.storeengine.shippingdetails.request.SaveShippingDetailsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ShippingDetailsService {

    private final ShippingDetailsRepository repository;

    /** The caller's saved shipping details, or null if they've never opted in to saving one. */
    public ShippingDetails get(String userId) {
        return repository.findByUserId(userId).orElse(null);
    }

    /** Upsert the caller's saved shipping details. */
    public ShippingDetails save(String userId, SaveShippingDetailsRequest patch) {
        ShippingDetails details = repository.findByUserId(userId).orElseGet(() -> {
            ShippingDetails created = new ShippingDetails();
            created.setUserId(userId);
            return created;
        });
        details.setFullName(patch.fullName());
        details.setEmail(patch.email());
        details.setAddress(patch.address());
        details.setCity(patch.city());
        details.setPostalCode(patch.postalCode());
        details.setUpdatedAt(Instant.now());
        return repository.save(details);
    }
}
