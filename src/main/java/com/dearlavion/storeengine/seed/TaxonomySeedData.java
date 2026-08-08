package com.dearlavion.storeengine.seed;

import java.util.List;

/** Direct port of dearlavion-store-engine's src/seed/taxonomy-seed.ts — today's hardcoded axis
 * option lists, preserved as seed data so a fresh v2 deploy behaves identically to v1 until an
 * admin edits something in Kit Settings. Destination/Season/Party's 'All' sentinel is deliberately
 * NOT seeded here — it's a structural query match value (ProductQueryBuilder's tag() uses
 * `$in: [value, 'All']`), not a real admin-editable option. */
public final class TaxonomySeedData {

    public record Entry(String axis, String value, Integer order, String emoji, String subtext, String code) {
        static Entry of(String axis, String value, int order) {
            return new Entry(axis, value, order, null, null, null);
        }

        static Entry of(String axis, String value, int order, String emoji) {
            return new Entry(axis, value, order, emoji, null, null);
        }
    }

    public static final List<Entry> ENTRIES = List.of(
            Entry.of("destination", "Beach", 0, "🏖️"),
            Entry.of("destination", "Mountain", 1, "⛰️"),
            Entry.of("destination", "City", 2, "🏙️"),

            Entry.of("season", "Summer", 0, "☀️"),
            Entry.of("season", "Winter", 1, "❄️"),
            Entry.of("season", "Rainy", 2, "🌧️"),

            Entry.of("party", "Solo", 0),
            Entry.of("party", "Group", 1),

            Entry.of("transportation", "Flight", 0),
            Entry.of("transportation", "Car", 1),
            Entry.of("transportation", "Train", 2),
            Entry.of("transportation", "Cruise", 3),

            Entry.of("activity", "Hiking", 0),
            Entry.of("activity", "Swimming", 1),
            Entry.of("activity", "Sightseeing", 2),
            Entry.of("activity", "Business", 3),
            Entry.of("activity", "Photography", 4),
            Entry.of("activity", "Nightlife", 5),
            Entry.of("activity", "Food", 6),
            Entry.of("activity", "Relaxing", 7),

            Entry.of("kitCategory", "Essentials", 0),
            Entry.of("kitCategory", "Clothing", 1),
            Entry.of("kitCategory", "Footwear", 2),
            Entry.of("kitCategory", "Toiletries", 3),
            Entry.of("kitCategory", "Beauty Kit", 4),
            Entry.of("kitCategory", "Tech Pack", 5),
            Entry.of("kitCategory", "Health & Safety", 6),
            Entry.of("kitCategory", "Weather Gear", 7),
            Entry.of("kitCategory", "Activity Gear", 8),
            Entry.of("kitCategory", "Comfort", 9),
            Entry.of("kitCategory", "Food & Hydration", 10),

            // Fixed cardinality — AdminTaxonomyController rejects add/delete on this axis; admin can
            // only rename value/subtext. `code` is the stable key the survey scoring's kit-size
            // lookup uses.
            new Entry("duration", "Day Tour", 0, null, "1 day", "day"),
            new Entry("duration", "Quick escape", 1, null, "2–4 days", "short"),
            new Entry("duration", "A proper break", 2, null, "1–2 weeks", "medium"),
            new Entry("duration", "Living it", 3, null, "3+ weeks", "long"),

            Entry.of("gender", "Woman", 0),
            Entry.of("gender", "Man", 1),
            Entry.of("gender", "Nonbinary", 2),
            Entry.of("gender", "Prefer not to say", 3)
    );

    private TaxonomySeedData() {
    }
}
