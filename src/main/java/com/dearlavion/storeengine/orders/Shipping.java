package com.dearlavion.storeengine.orders;

import lombok.Getter;
import lombok.Setter;

/** Recipient details captured at checkout — where an order ships. */
@Getter
@Setter
public class Shipping {
    private String fullName;
    private String email;
    private String address;
    private String city;
    private String postalCode;
}
