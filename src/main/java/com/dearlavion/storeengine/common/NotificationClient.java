package com.dearlavion.storeengine.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

/** Shared client for server-to-server calls into dearlavion-notification-service's
 * X-Internal-Api-Key-gated endpoints (newsletter thank-you, popular-kit announcements — see
 * NewsletterService and PopularKitService). Every call is dispatched off the request thread via
 * CompletableFuture — a slow SMTP send (or a batch of them) or a notification-service outage
 * should never add latency to, or fail, the caller's own response. */
@Slf4j
@Component
public class NotificationClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.notification-service-url}")
    private String notificationServiceUrl;

    @Value("${app.internal-api-key}")
    private String internalApiKey;

    public void postAsync(String path, Object body) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Internal-Api-Key", internalApiKey);
                restTemplate.postForObject(notificationServiceUrl + path, new HttpEntity<>(body, headers), Void.class);
            } catch (Exception e) {
                log.error("notification-service call to {} failed: {}", path, e.getMessage());
            }
        });
    }
}
