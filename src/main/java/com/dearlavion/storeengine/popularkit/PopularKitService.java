package com.dearlavion.storeengine.popularkit;

import com.dearlavion.storeengine.common.Slugify;
import com.dearlavion.storeengine.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PopularKitService {

    private final PopularKitRepository repository;

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
        Instant now = Instant.now();
        PopularKit kit = new PopularKit();
        kit.setName(dto.name().trim());
        kit.setSlug(uniqueSlug(dto.name()));
        kit.setTag(dto.tag() != null ? dto.tag() : "");
        kit.setImage(dto.image() != null ? dto.image() : "");
        kit.setDestination(dto.destination() != null ? dto.destination() : "");
        kit.setSeason(dto.season() != null ? dto.season() : "");
        kit.setParty(dto.party() != null ? dto.party() : "");
        kit.setDuration(dto.duration() != null ? dto.duration() : "");
        kit.setProductIds(dto.productIds() != null ? dto.productIds() : List.of());
        kit.setActive(true);
        kit.setCreatedAt(now);
        kit.setUpdatedAt(now);
        return repository.save(kit);
    }

    /** Slug stays stable across renames (it's the curated id), matching how products behave. */
    public PopularKit update(String id, UpdatePopularKitRequest dto) {
        PopularKit kit = getByIdOrSlug(id);
        if (dto.name() != null) kit.setName(dto.name().trim());
        if (dto.tag() != null) kit.setTag(dto.tag());
        if (dto.image() != null) kit.setImage(dto.image());
        if (dto.destination() != null) kit.setDestination(dto.destination());
        if (dto.season() != null) kit.setSeason(dto.season());
        if (dto.party() != null) kit.setParty(dto.party());
        if (dto.duration() != null) kit.setDuration(dto.duration());
        if (dto.productIds() != null) kit.setProductIds(dto.productIds());
        kit.setUpdatedAt(Instant.now());
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
