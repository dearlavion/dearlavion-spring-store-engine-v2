package com.dearlavion.storeengine.profile.request;

import jakarta.validation.constraints.Size;

/** `currency`, if present, must be one of Currency.SUPPORTED_CURRENCIES — checked in
 * ProfileService rather than via a Bean Validation annotation here (no built-in "value in this
 * Java list constant" constraint without a custom validator class). */
public record UpdateProfileRequest(
        @Size(max = 60) String displayName,
        @Size(max = 16) String avatar,
        String currency
) {
}
