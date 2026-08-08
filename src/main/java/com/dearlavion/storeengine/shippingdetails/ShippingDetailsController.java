package com.dearlavion.storeengine.shippingdetails;

import com.dearlavion.storeengine.security.AuthenticatedUser;
import com.dearlavion.storeengine.shippingdetails.model.ShippingDetails;
import com.dearlavion.storeengine.shippingdetails.request.SaveShippingDetailsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Caller's own opt-in saved shipping details — prefills future checkouts. */
@RestController
@RequestMapping("/shipping-details")
@RequiredArgsConstructor
public class ShippingDetailsController {

    private final ShippingDetailsService service;

    @GetMapping
    public ShippingDetails get(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.get(user.userId());
    }

    @PutMapping
    public ShippingDetails save(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody SaveShippingDetailsRequest dto) {
        return service.save(user.userId(), dto);
    }
}
