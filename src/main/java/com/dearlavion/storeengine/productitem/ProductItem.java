package com.dearlavion.storeengine.productitem;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A purchasable SKU under a generic Product. Every Product has at least one ProductItem; a
 * product with real brand variants just has more than one. Price, currency, image, and inventory
 * all live here — Product itself is purely the template/generic. */
@Getter
@Setter
@Document(collection = "product_items")
public class ProductItem {

    @Id
    private String id;

    @Indexed
    private String productId;

    /** Blank = this item is the product's sole/default variant (no real "brand" to show). */
    private String brand;

    /** Size tier for products that come in sizes (1 = travel/mini, 2 = standard, 3 = large).
     * Unset = not size-differentiated. */
    private Integer sizeTier;

    private String sizeLabel;

    private String name;

    private double price;

    private String currency = "USD";

    private String image;

    /** Up to 5 additional photos for this brand/SKU's gallery. */
    private List<String> images = new ArrayList<>();

    /** Up to 5 influencer/testimonial videos for this brand/SKU. */
    private List<ProductVideo> videos = new ArrayList<>();

    private String icon;

    private int stock = 0;

    private boolean soldOut = false;

    /** Raw admin-authored discount inputs — `price` never changes meaning (still the base/list
     * price); the actual discounted amount is computed at read time in the public listing pipeline
     * (see ProductItemQueryBuilder), which projects `price` as the discounted figure and adds
     * `originalPrice` for the "was $X" comparison. */
    private boolean onSale = false;

    /** "percent" | "amount" */
    private String discountType;

    private Double discountValue;

    @Indexed
    private boolean active = true;

    private Instant createdAt;

    private Instant updatedAt;
}
