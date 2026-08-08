package com.dearlavion.storeengine.newsletter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** An email opted in to product news/blog updates — currently surfaced as an optional prompt on
 * the My Kit page's "Download PDF" action, not a customer-account feature (no auth required).
 * Resubscribing with the same email is a harmless no-op, not a conflict. */
@Getter
@Setter
@Document(collection = "newsletter_subscribers")
public class NewsletterSubscriber {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private Instant subscribedAt;
}
