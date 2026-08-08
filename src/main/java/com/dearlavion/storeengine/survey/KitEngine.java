package com.dearlavion.storeengine.survey;

import com.dearlavion.storeengine.survey.model.EngineProduct;
import com.dearlavion.storeengine.survey.model.KitAnswers;
import com.dearlavion.storeengine.survey.model.KitPick;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Catalog-driven kit recommendation engine — direct port of kit-engine.ts. Scores every active
 * product against the trip answers, excludes specific-mismatches, then selects a category-covering
 * kit sized to the trip. Pure &amp; deterministic (no randomness) so saved surveys are reproducible.
 */
public final class KitEngine {

    private KitEngine() {
    }

    // Keyed on Duration's stable `code`, not its admin-editable display label.
    private static final Map<String, Integer> TARGET_BY_DURATION = Map.of("short", 14, "medium", 20, "long", 26);
    private static final int MIN_KIT = 10;
    private static final int MAX_KIT = 30;
    private static final int PURE_CORE_CAP = 4; // how many generic "fits everything" items may fill the kit

    private static final double AXIS_MATCH = 2; // product tagged with the specific answer — defines the kit
    private static final double AXIS_ALL = 0.5; // product tagged 'All'/untagged — eligible but dampened
    private static final double AXIS_MISMATCH = Double.NEGATIVE_INFINITY; // tagged only for other values — excluded
    private static final double ACTIVITY_WEIGHT = 3; // per overlapping activity — the strongest differentiator
    private static final double TRANSPORT_WEIGHT = 1.5; // soft boost, not exclusionary
    // Matches kit-recommendation.ts's mock-mode weight — kitCategory is "weighted heaviest".
    private static final double KIT_CATEGORY_WEIGHT = 5;

    private static final Pattern LONG_TRIP_PATTERN = Pattern.compile("laundry|packing|cube|organizer|detergent|duffel");
    private static final Pattern GROUP_PATTERN = Pattern.compile("group|shared|large|hub|multi|family");
    private static final Pattern BEACH_RAINY_PATTERN = Pattern.compile("dry|waterproof|pouch");

    /** Tri-state fit for one axis. */
    private static double axisScore(List<String> tags, String answer) {
        if (tags.contains(answer)) return AXIS_MATCH;
        if (tags.isEmpty() || tags.contains("All")) return AXIS_ALL;
        return AXIS_MISMATCH;
    }

    /** Same tri-state fit, but for a multi-select axis (currently just destinations). An empty
     * answer list means the shopper picked "All" — every product is neutral on this axis. */
    private static double multiAxisScore(List<String> tags, List<String> answers) {
        if (answers == null || answers.isEmpty()) return AXIS_ALL;
        if (tags.stream().anyMatch(answers::contains)) return AXIS_MATCH;
        if (tags.isEmpty() || tags.contains("All")) return AXIS_ALL;
        return AXIS_MISMATCH;
    }

    private static int activityOverlap(EngineProduct product, List<String> selected) {
        if (selected == null || selected.isEmpty() || product.activities().isEmpty()) return 0;
        return (int) product.activities().stream().filter(selected::contains).count();
    }

    /** Soft boost only — null/omitted transportModes means unrestricted; unlike axis tags, this
     * never excludes a product, only nudges it up. */
    private static double transportBoost(EngineProduct product, String answer) {
        if (answer == null || answer.isBlank() || product.transportModes().isEmpty()) return 0;
        return product.transportModes().contains(answer) ? TRANSPORT_WEIGHT : 0;
    }

    private static double kitCategoryBoost(EngineProduct product, List<String> selected) {
        if (selected == null || selected.isEmpty() || product.kitCategory() == null) return 0;
        return selected.contains(product.kitCategory()) ? KIT_CATEGORY_WEIGHT : 0;
    }

    private static int effectivePartySize(KitAnswers a) {
        if (a.partySize() != null) return a.partySize();
        return "Group".equals(a.party()) ? 4 : 1;
    }

    /** Small cross-axis nudges so adjacent answers diverge. */
    private static double interactionBoost(EngineProduct product, KitAnswers a) {
        String hay = (product.name() + " " + (product.category() != null ? product.category() : "")).toLowerCase();
        double b = 0;
        if ("long".equals(a.duration()) || "medium".equals(a.duration())) {
            if (LONG_TRIP_PATTERN.matcher(hay).find()) b += 1.5;
        }
        if (effectivePartySize(a) >= 3) {
            if (GROUP_PATTERN.matcher(hay).find()) b += 1.5;
        }
        if (a.destinations() != null && a.destinations().contains("Beach") && "Rainy".equals(a.season())) {
            if (BEACH_RAINY_PATTERN.matcher(hay).find()) b += 1;
        }
        return b;
    }

    /** pureCore = eligible only via generic (All/neutral) tags with no activity/transport/kit-
     * category match and no interaction relevance — i.e. a fits-everything item. */
    private record Scored(EngineProduct product, double score, boolean pureCore) {
    }

    private static Scored score(EngineProduct product, KitAnswers a) {
        double d = multiAxisScore(product.destinations(), a.destinations());
        double s = axisScore(product.seasons(), a.season());
        double p = axisScore(product.parties(), a.party());
        if (d == AXIS_MISMATCH || s == AXIS_MISMATCH || p == AXIS_MISMATCH) return null; // excluded

        int overlap = activityOverlap(product, a.activities());
        double boost = interactionBoost(product, a);
        double transport = transportBoost(product, a.transportation());
        double kitCat = kitCategoryBoost(product, a.priorityCategories());
        double total = d + s + p + overlap * ACTIVITY_WEIGHT + boost + transport + kitCat;
        if (product.popular()) total += 0.8;
        if (product.tested()) total += 0.5;

        boolean pureCore = d == AXIS_ALL && s == AXIS_ALL && p == AXIS_ALL
                && overlap == 0 && boost == 0 && transport == 0 && kitCat == 0;
        return new Scored(product, total, pureCore);
    }

    private static int targetSize(KitAnswers a) {
        int base = TARGET_BY_DURATION.getOrDefault(a.duration(), 18);
        int size = effectivePartySize(a);
        int extra = size >= 3 ? Math.min((size - 2) / 2, 4) : 0;
        return Math.max(MIN_KIT, Math.min(MAX_KIT, base + extra));
    }

    /**
     * Build a ranked, category-covering kit. Selection: (1) the best item of each meaningfully-
     * fitting category for breadth, then (2) fill by score, capping generic "fits everything" items
     * so specific picks define the kit.
     */
    public static List<KitPick> buildKit(KitAnswers answers, List<EngineProduct> products) {
        List<Scored> eligible = products.stream()
                .map(p -> score(p, answers))
                .filter(Objects::nonNull)
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();

        int target = targetSize(answers);
        List<Scored> chosen = new ArrayList<>();
        Set<String> takenIds = new HashSet<>();

        List<Scored> specifics = eligible.stream().filter(x -> !x.pureCore()).toList(); // earned a real fit

        // 1 — essentials: the top few generic "fits everything" items. Capped so generics don't
        // define the kit.
        for (Scored x : eligible.stream().filter(Scored::pureCore).limit(PURE_CORE_CAP).toList()) {
            if (chosen.size() >= target) break;
            chosen.add(x);
            takenIds.add(x.product().id());
        }

        // 2 — breadth: the best specific item of each category, so the kit spans the trip's needs.
        Map<String, Scored> bestByCategory = new LinkedHashMap<>();
        for (Scored x : specifics) {
            String cat = x.product().category() != null ? x.product().category() : "Other";
            bestByCategory.putIfAbsent(cat, x);
        }
        for (Scored x : bestByCategory.values()) {
            if (chosen.size() >= target) break;
            if (takenIds.add(x.product().id())) chosen.add(x);
        }

        // 3 — depth: fill remaining slots with the highest-scoring specifics not yet taken.
        for (Scored x : specifics) {
            if (chosen.size() >= target) break;
            if (takenIds.add(x.product().id())) chosen.add(x);
        }

        return chosen.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .map(x -> new KitPick(x.product().name(), x.product().id(), x.product().category()))
                .collect(Collectors.toList());
    }
}
