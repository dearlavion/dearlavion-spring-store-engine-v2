package com.dearlavion.storeengine.popularkit.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** An admin-curated "Popular kit" shown on the storefront homepage. `productIds` is the sole
 * source of what's in the kit (admin-picked); destination/season/party/duration are cosmetic
 * badges only. */
@Getter
@Setter
@Document(collection = "popular_kits")
public class PopularKit {

    @Id
    private String id;

    private String name;

    /** Stable, URL-friendly id used for idempotent seeding + lookups. */
    @Indexed(unique = true)
    private String slug;

    private String tag = "";

    private String image = "";

    private String destination = "";

    private String season = "";

    private String party = "";

    private String duration = "";

    private List<String> productIds = new ArrayList<>();

    @Indexed
    private boolean active = true;

    private Instant createdAt;

    private Instant updatedAt;
}
