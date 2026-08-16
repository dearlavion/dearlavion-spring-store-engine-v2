package com.dearlavion.storeengine.product.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/**
 * A generic/template product — the stable concept a packing-list slot points at. Purely
 * presentational + taxonomy: name, category, description, tags. Purchase data (price/currency/
 * image/stock/soldOut) lives on its ProductItem children instead.
 *
 * `id` is a slugified-name string (e.g. "passport-wallet"), not an auto-generated ObjectId — set
 * once at creation (ProductService.create()) and never changed afterward. This also means the id
 * doubles as the slug: ProductItem.productId, PopularKit.productIds, Product.linkedProductIds all
 * just reference this same string.
 */
@Getter
@Setter
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    private String name;

    @Indexed
    private String category;

    private String description;

    private String icon;

    private boolean popular = false;

    private boolean tested = false;

    private List<String> destinations = new ArrayList<>(List.of("All"));

    private List<String> seasons = new ArrayList<>(List.of("All"));

    private List<String> parties = new ArrayList<>(List.of("All"));

    private List<String> activities = new ArrayList<>();

    private List<String> transportModes = new ArrayList<>();

    /** Trip lengths this product suits, as Duration's stable `code` ('day'|'short'|'medium'|'long')
     * rather than the admin-editable display label — same reason KitEngine keys its size lookup on
     * the code. Empty = suits any length; soft boost only, never exclusionary. */
    private List<String> durations = new ArrayList<>();

    /** Who this product is for, as Gender's display value. Empty = suits anyone; soft boost only. */
    private List<String> genders = new ArrayList<>();

    /** The packing-list buckets this product belongs to — powers the survey's "what matters most
     * to you" question. Ordered: the first is the primary one the storefront groups a kit by, the
     * rest widen what the product matches. Not required at the schema level — enforcing it would
     * break saving any other field on every product that predates this field. The admin form is
     * the real enforcement point. */
    private List<String> kitCategories = new ArrayList<>();

    @Indexed
    private boolean active = true;

    /**
     * Values for admin-created master-data collections, keyed by collection key
     * ({@code {"fabric": ["Linen"], "color": ["Navy"]}}). The nine built-in axes above stay typed
     * columns because the engine gives each bespoke meaning; anything registered at runtime has no
     * column to live in, so it lands here. Empty for a product tagged only on the built-ins.
     */
    private Map<String, List<String>> tags = new LinkedHashMap<>();

    /** Admin-curated explicit "suggested with" links (product ids). */
    private List<String> linkedProductIds = new ArrayList<>();

    private Instant createdAt;

    private Instant updatedAt;
}
