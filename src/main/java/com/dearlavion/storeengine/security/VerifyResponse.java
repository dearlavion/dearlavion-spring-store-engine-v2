package com.dearlavion.storeengine.security;

import lombok.Data;

/** Response shape of auth-service's POST /auth/verify. */
@Data
public class VerifyResponse {
    private boolean valid;
    private String username;
    private String email;
    private String userId;
    private String activeProfile;
    private String customer;
}
