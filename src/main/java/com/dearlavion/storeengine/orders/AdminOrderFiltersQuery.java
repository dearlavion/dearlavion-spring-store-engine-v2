package com.dearlavion.storeengine.orders;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminOrderFiltersQuery {
    @Pattern(regexp = "UNPAID|PENDING|PAID|REJECTED")
    private String paymentStatus;

    @Pattern(regexp = "Processing|Shipped|Delivered")
    private String deliveryStatus;
}
