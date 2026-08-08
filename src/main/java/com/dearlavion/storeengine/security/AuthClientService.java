package com.dearlavion.storeengine.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** Verifies bearer tokens against the Dearlavion auth-service — ported verbatim from the NestJS
 * AuthClientService (src/auth/auth.ts). No JWT library needed here: verification is delegated to
 * a remote POST /auth/verify call, not local decoding. */
@Slf4j
@Service
public class AuthClientService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.auth-server-url}")
    private String authServerUrl;

    /** Tokens whose `customer` claim differs from this are rejected (tenant isolation). Empty = disabled. */
    @Value("${app.expected-customer:}")
    private String expectedCustomer;

    public String expectedCustomer() {
        return expectedCustomer;
    }

    /** Returns null on any failure (invalid token, auth-service unreachable) — fails closed, same as NestJS. */
    public VerifyResponse verify(String token) {
        String url = authServerUrl + "/auth/verify";
        try {
            return restTemplate.postForObject(url, Map.of("token", token), VerifyResponse.class);
        } catch (Exception e) {
            log.error("verify failed at {}: {}", url, e.getMessage());
            return null;
        }
    }
}
