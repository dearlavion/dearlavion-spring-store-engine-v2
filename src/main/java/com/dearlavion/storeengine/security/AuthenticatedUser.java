package com.dearlavion.storeengine.security;

/** Populated onto the SecurityContext as the Authentication principal once a Bearer token verifies. */
public record AuthenticatedUser(
        String userId,
        String username,
        String email,
        /** auth-service `activeProfile` role (ADMIN | STAFF | USER, legacy WISHER/COPILOT/BUSINESS_OWNER). */
        String role,
        /** auth-service tenant this identity belongs to. */
        String customer
) {
}
