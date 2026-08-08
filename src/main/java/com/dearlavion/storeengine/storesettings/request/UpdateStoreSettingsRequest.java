package com.dearlavion.storeengine.storesettings.request;

import jakarta.validation.constraints.Min;

public record UpdateStoreSettingsRequest(
        @Min(0) Double freeShippingMinimum,
        @Min(0) Double shippingFee
) {
}
