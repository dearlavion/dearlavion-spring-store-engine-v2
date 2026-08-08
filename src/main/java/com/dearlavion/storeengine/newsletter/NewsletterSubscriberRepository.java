package com.dearlavion.storeengine.newsletter;

import com.dearlavion.storeengine.newsletter.model.NewsletterSubscriber;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NewsletterSubscriberRepository extends MongoRepository<NewsletterSubscriber, String> {
    Optional<NewsletterSubscriber> findByEmail(String email);
}
