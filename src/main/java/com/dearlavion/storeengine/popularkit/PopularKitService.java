package com.dearlavion.storeengine.popularkit;

import com.dearlavion.storeengine.common.Slugify;
import com.dearlavion.storeengine.common.exception.NotFoundException;
import com.dearlavion.storeengine.popularkit.model.PopularKit;
import com.dearlavion.storeengine.popularkit.request.CreatePopularKitRequest;
import com.dearlavion.storeengine.popularkit.request.UpdatePopularKitRequest;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PopularKitService {

    private final PopularKitRepository repository;
    private final PopularKitMapper mapper;

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
        return repository.save(mapper.toEntity(dto, uniqueSlug(dto.name())));
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
