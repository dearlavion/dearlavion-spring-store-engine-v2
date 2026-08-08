package com.dearlavion.storeengine.popularkit;

import com.dearlavion.storeengine.popularkit.model.PopularKit;
import com.dearlavion.storeengine.popularkit.request.CreatePopularKitRequest;
import com.dearlavion.storeengine.popularkit.request.UpdatePopularKitRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class PopularKitMapper {

    public PopularKit toEntity(CreatePopularKitRequest dto, String slug) {
        Instant now = Instant.now();
        PopularKit kit = new PopularKit();
        kit.setName(dto.name().trim());
        kit.setSlug(slug);
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
        return kit;
    }

    /** Slug stays stable across renames (it's the curated id), matching how products behave. */
    public void applyPatch(PopularKit kit, UpdatePopularKitRequest dto) {
        if (dto.name() != null) kit.setName(dto.name().trim());
        if (dto.tag() != null) kit.setTag(dto.tag());
        if (dto.image() != null) kit.setImage(dto.image());
        if (dto.destination() != null) kit.setDestination(dto.destination());
        if (dto.season() != null) kit.setSeason(dto.season());
        if (dto.party() != null) kit.setParty(dto.party());
        if (dto.duration() != null) kit.setDuration(dto.duration());
        if (dto.productIds() != null) kit.setProductIds(dto.productIds());
        kit.setUpdatedAt(Instant.now());
    }
}
