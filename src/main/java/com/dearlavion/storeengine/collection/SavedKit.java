package com.dearlavion.storeengine.collection;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A named, saved travel kit — backs /profile/collection.
 *
 * `id` is a slugified-name string (e.g. "beach-trip"), not an auto-generated ObjectId — set once
 * at creation ({@link CollectionService#uniqueId}, scoped per-user since two different users can
 * each have a kit named "My kit") and never changed afterward, same convention Product.id uses.
 */
@Getter
@Setter
@Document(collection = "saved_kits")
public class SavedKit {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String name = "My kit";

    private BuiltKit kit;

    private Instant savedAt;
}
