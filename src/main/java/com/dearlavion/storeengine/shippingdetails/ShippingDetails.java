package com.dearlavion.storeengine.shippingdetails;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** One saved shipping detail per user (opt-in at checkout) — reused to prefill future checkouts.
 * Unlike a profile, there's no sensible non-empty default, so no document is auto-created on
 * read; it only exists once a shopper has actually opted in and saved one. */
@Getter
@Setter
@Document(collection = "shipping_details")
public class ShippingDetails {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private String fullName;
    private String email;
    private String address;
    private String city;
    private String postalCode;

    private Instant updatedAt;
}
