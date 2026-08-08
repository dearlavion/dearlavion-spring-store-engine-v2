package com.dearlavion.storeengine.popularkit;

import com.dearlavion.storeengine.common.NotificationClient;
import com.dearlavion.storeengine.common.Slugify;
import com.dearlavion.storeengine.common.exception.NotFoundException;
import com.dearlavion.storeengine.newsletter.NewsletterSubscriberRepository;
import com.dearlavion.storeengine.newsletter.model.NewsletterSubscriber;
import com.dearlavion.storeengine.popularkit.model.PopularKit;
import com.dearlavion.storeengine.popularkit.request.CreatePopularKitRequest;
import com.dearlavion.storeengine.popularkit.request.UpdatePopularKitRequest;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PopularKitService {

    private final PopularKitRepository repository;
    private final PopularKitMapper mapper;
    private final NewsletterSubscriberRepository newsletterRepository;
    private final NotificationClient notificationClient;

    /** Public homepage listing — active kits only, oldest first (stable curation order). */
    public List<PopularKit> listPublic() {
        return repository.findByActiveTrueOrderByCreatedAtAsc();
    }

    /** Admin listing — includes deactivated kits. */
    public List<PopularKit> listAll() {
        return repository.findAllByOrderByCreatedAtAsc();
    }

    public PopularKit getByIdOrSlug(String idOrSlug) {
        if (ObjectId.isValid(idOrSlug)) {
            return repository.findById(idOrSlug).orElseThrow(() -> new NotFoundException("Popular kit not found"));
        }
        return repository.findBySlug(idOrSlug).orElseThrow(() -> new NotFoundException("Popular kit not found"));
    }

    public PopularKit create(CreatePopularKitRequest dto) {
        PopularKit kit = repository.save(mapper.toEntity(dto, uniqueSlug(dto.name())));
        announceNewKit(kit);
        return kit;
    }

    /** Notifies every current newsletter subscriber that a new Popular Kit was just published —
     * once per creation, never for update()/deactivate(). Fire-and-forget via NotificationClient,
     * so a slow batch send (or notification-service being down) never delays the admin's response. */
    private void announceNewKit(PopularKit kit) {
        List<String> emails = newsletterRepository.findAll().stream().map(NewsletterSubscriber::getEmail).toList();
        if (emails.isEmpty()) return;
        notificationClient.postAsync("/notification/internal/popular-kit-announcement", Map.of(
                "emails", emails,
                "kitName", kit.getName(),
                "kitSlug", kit.getSlug(),
                "image", kit.getImage(),
                "tag", kit.getTag()
        ));
    }

    /** Slug stays stable across renames (it's the curated id), matching how products behave. */
    public PopularKit update(String id, UpdatePopularKitRequest dto) {
        PopularKit kit = getByIdOrSlug(id);
        mapper.applyPatch(kit, dto);
        return repository.save(kit);
    }

    /** Soft delete (active=false), matching the product convention. */
    public void deactivate(String id) {
        PopularKit kit = getByIdOrSlug(id);
        kit.setActive(false);
        kit.setUpdatedAt(Instant.now());
        repository.save(kit);
    }

    private String uniqueSlug(String name) {
        String base = Slugify.slugify(name);
        if (base.isBlank()) base = "kit";
        String slug = base;
        for (int i = 2; repository.existsBySlug(slug); i++) {
            slug = base + "-" + i;
        }
        return slug;
    }
}
