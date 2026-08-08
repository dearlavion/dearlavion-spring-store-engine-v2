package com.dearlavion.storeengine.orders.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdatePaymentStatusRequest(
        @NotBlank @Pattern(regexp = "UNPAID|PENDING|PAID|REJECTED") String status,
        String paymentId
) {
}
