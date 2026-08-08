package com.dearlavion.storeengine.newsletter;

import com.dearlavion.storeengine.common.NotificationClient;
import com.dearlavion.storeengine.newsletter.model.NewsletterSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterSubscriberRepository repository;
    private final NotificationClient notificationClient;

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
                notificationClient.postAsync("/notification/internal/newsletter-thanks", Map.of("email", normalized));
            } catch (DuplicateKeyException ignored) {
                // Another concurrent request just inserted the same email — treat as a resubscribe,
                // not a new one (that request already triggered the thank-you email).
                alreadySubscribed = true;
            }
        }
        return Map.of("subscribed", true, "alreadySubscribed", alreadySubscribed);
    }
}
