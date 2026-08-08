package com.dearlavion.storeengine.newsletter;

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

    /** Upsert by email — subscribing twice (e.g. a repeat PDF download, or a race between two
     * concurrent requests for the same email) is a no-op, not an error. */
    public Map<String, Boolean> subscribe(String email) {
        String normalized = email.trim().toLowerCase();
        if (repository.findByEmail(normalized).isEmpty()) {
            NewsletterSubscriber subscriber = new NewsletterSubscriber();
            subscriber.setEmail(normalized);
            subscriber.setSubscribedAt(Instant.now());
            try {
                repository.save(subscriber);
            } catch (DuplicateKeyException ignored) {
                // Another concurrent request just inserted the same email — already subscribed.
            }
        }
        return Map.of("subscribed", true);
    }
}
