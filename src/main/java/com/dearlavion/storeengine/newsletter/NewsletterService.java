package com.dearlavion.storeengine.newsletter;

import com.dearlavion.storeengine.newsletter.model.NewsletterSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
public class NewsletterService {

    private final NewsletterSubscriberRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.notification-service-url}")
    private String notificationServiceUrl;

    @Value("${app.internal-api-key}")
    private String internalApiKey;

    public NewsletterService(NewsletterSubscriberRepository repository) {
        this.repository = repository;
    }

    /** Upsert by email — subscribing twice (e.g. a repeat PDF download, or a race between two
     * concurrent requests for the same email) is a no-op, not an error, and never re-sends the
     * thank-you email. Reports whether this email was already on the list so the caller can show
     * a different message for a resubscribe vs. a genuinely new signup. */
    public Map<String, Boolean> subscribe(String email) {
        String normalized = email.trim().toLowerCase();
        boolean alreadySubscribed = repository.findByEmail(normalized).isPresent();

        if (!alreadySubscribed) {
            NewsletterSubscriber subscriber = new NewsletterSubscriber();
            subscriber.setEmail(normalized);
            subscriber.setSubscribedAt(Instant.now());
            try {
                repository.save(subscriber);
                sendThanksEmail(normalized);
            } catch (DuplicateKeyException ignored) {
                // Another concurrent request just inserted the same email — treat as a resubscribe,
                // not a new one (that request already triggered the thank-you email).
                alreadySubscribed = true;
            }
        }
        return Map.of("subscribed", true, "alreadySubscribed", alreadySubscribed);
    }

    /** Fire-and-forget: a notification-service outage should never fail the subscribe request
     * itself — the subscriber is already saved by the time this runs. */
    private void sendThanksEmail(String email) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            restTemplate.postForObject(
                    notificationServiceUrl + "/notification/internal/newsletter-thanks",
                    new HttpEntity<>(Map.of("email", email), headers),
                    Void.class
            );
        } catch (Exception e) {
            log.error("newsletter thank-you email failed for {}: {}", email, e.getMessage());
        }
    }
}
