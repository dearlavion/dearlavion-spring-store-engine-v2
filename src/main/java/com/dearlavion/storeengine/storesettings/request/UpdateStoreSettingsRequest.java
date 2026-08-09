package com.dearlavion.storeengine.storesettings.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record UpdateStoreSettingsRequest(
        @Min(0) Double freeShippingMinimum,
        @Min(0) Double shippingFee,
        @Pattern(regexp = "s3|drive") String defaultMediaProvider,
        @Min(1) Integer maxImageSizeKb
) {
}
