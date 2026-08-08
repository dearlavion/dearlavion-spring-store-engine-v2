package com.dearlavion.storeengine.collection;

import com.dearlavion.storeengine.common.Slugify;
import com.dearlavion.storeengine.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final SavedKitRepository repository;

    /** The caller's saved kits, newest first. */
    public List<SavedKit> listForUser(String userId) {
        return repository.findByUserIdOrderBySavedAtDesc(userId);
    }

    public SavedKit save(String userId, String name, BuiltKitRequest kitDto) {
        String cleanName = name.trim().isEmpty() ? "My kit" : name.trim();
        BuiltKit kit = new BuiltKit();
        kit.setItems(toKitItems(kitDto.items()));
        kit.setSummary(kitDto.summary() != null ? kitDto.summary() : "");
        kit.setTitle(kitDto.title());

        SavedKit savedKit = new SavedKit();
        savedKit.setId(uniqueId(cleanName));
        savedKit.setUserId(userId);
        savedKit.setName(cleanName);
        savedKit.setKit(kit);
        savedKit.setSavedAt(Instant.now());
        return repository.save(savedKit);
    }

    /** Mongo's `_id` uniqueness is always collection-wide, not scopable to `{_id, userId}` — a
     * per-user-scoped collision check can return an id another user already holds, which then
     * fails with a duplicate-key error on insert. So this checks globally, exactly like
     * ProductService.uniqueId(); the only user-visible effect is an occasional cosmetic `-2` suffix
     * if two different users happen to save a kit with the same name — listForUser()/delete()
     * still scope by userId separately, so nobody ever sees or touches another user's kit either way. */
    private String uniqueId(String name) {
        String base = Slugify.slugify(name);
        String id = base;
        for (int i = 2; repository.existsById(id); i++) {
            id = base + "-" + i;
        }
        return id;
    }

    /** Update this kit's name and/or item list in place — scoped the same as delete() so a user
     * can only ever mutate their own kit. Renaming here (not a fresh save()) is what keeps
     * "editing an already-saved kit" from creating a duplicate entry. */
    public SavedKit update(String userId, String id, UpdateSavedKitRequest patch) {
        SavedKit kit = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Saved kit not found"));
        if (patch.name() != null) {
            kit.setName(patch.name().trim().isEmpty() ? "My kit" : patch.name().trim());
        }
        if (patch.items() != null) {
            kit.getKit().setItems(toKitItems(patch.items()));
        }
        return repository.save(kit);
    }

    public void delete(String userId, String id) {
        if (repository.deleteByIdAndUserId(id, userId) == 0) {
            throw new NotFoundException("Saved kit not found");
        }
    }

    private static List<KitItem> toKitItems(List<KitItemRequest> items) {
        List<KitItem> result = new ArrayList<>();
        for (KitItemRequest i : items) {
            KitItem item = new KitItem();
            item.setLabel(i.label());
            item.setProductId(i.productId());
            result.add(item);
        }
        return result;
    }
}
