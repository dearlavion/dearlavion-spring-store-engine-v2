package com.dearlavion.storeengine.survey;

import com.dearlavion.storeengine.survey.model.EngineProduct;
import com.dearlavion.storeengine.survey.model.KitAnswers;
import com.dearlavion.storeengine.survey.model.KitPick;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
    private static final Map<String, Integer> TARGET_BY_DURATION = Map.of("day", 10, "short", 14, "medium", 20, "long", 26);
    private static final int MIN_KIT = 10;
    private static final int MAX_KIT = 30;

    private static final double AXIS_MATCH = 2; // product tagged with the specific answer — defines the kit
    private static final double AXIS_ALL = 0.5; // product tagged 'All'/untagged — eligible but dampened
    private static final double AXIS_MISMATCH = Double.NEGATIVE_INFINITY; // tagged only for other values — excluded
    private static final double ACTIVITY_WEIGHT = 3; // per overlapping activity
    // Capped for the same reason kit category is: a product tagged into many activities shouldn't
    // win on breadth of tagging. Uncapped this reached +9 and outbid an explicit kit request (5-8),
    // which is how a swimsuit nobody asked for outranked the toiletry kit they did ask for.
    private static final double ACTIVITY_CAP = 6;
    /**
     * Transport, trip length and gender: attributes of the trip rather than something the shopper
     * asked to prioritise. One weight, because they were always the same number and three names
     * implied a distinction that never existed. An untagged product suits any of them, so tagging
     * can only ever help a product, never hide it.
     */
    private static final double TRIP_SIGNAL_WEIGHT = 1.5;
    // Kit category is the heaviest signal — it's the one question asked purely to rank. Graded so
    // covering more of what the shopper asked for beats scraping in on one bucket, but with
    // diminishing returns and a hard cap, so a product tagged into every kit can't win on breadth
    // of tagging alone. 1 match = 5, 2 = 6.5, 3+ = 8.
    private static final double KIT_CATEGORY_WEIGHT = 5;      // first matching kit
    private static final double KIT_CATEGORY_EXTRA = 1.5;     // each further matching kit
    private static final double KIT_CATEGORY_CAP = 8;         // reached at 3 matches
    /**
     * The basics everyone packs — the broadest bucket in the catalog, so it never earns a place on
     * its own. A product must match one of the shopper's <em>other</em> picked kits first; this
     * then adds to that. A laundry bag tagged {@code [Essentials Kit, Laundry Kit]} is packed when
     * Laundry was asked for, not merely because Essentials was ticked alongside six other things.
     */
    private static final String BASELINE_KIT_CATEGORY = "Essentials Kit";
    /**
     * Most of the finished kit that may be products the shopper didn't ask for. They still earned
     * their place (an activity or destination match), but a kit is defined by what was requested.
     *
     * <p>Deliberately a share of <em>the kit as built</em>, not of the target size. Target comes from
     * trip length, so on a long trip it's 26 regardless of whether the shopper asked for one kit or
     * six — measuring against it left the related tier a budget it could never exhaust, and a
     * one-kit survey still came back two-thirds full of things nobody asked for. Measuring against
     * what was actually requested makes the kit scale with demand instead of with the calendar.
     */
    private static final double RELATED_SHARE = 0.4;
    /**
     * ...but a strict share starves the narrow cases: one requested item earns a budget of zero, so
     * asking for a thinly-stocked kit returned a kit of one. The point is that related items can't
     * *dominate*, not that they're forbidden, so a small allowance survives regardless. This is the
     * one case where related may outnumber requested, and it's the case where the kit would
     * otherwise be too thin to be worth building.
     */
    private static final int MIN_RELATED = 2;
    /**
     * Most items sharing one product category, so a kit can't come back as five near-identical
     * travel accessories. Scales with kit size (10 -> 3, 26 -> 5) rather than binding hard on the
     * long trips that legitimately want more of everything.
     */
    private static final int MIN_CATEGORY_CAP = 3;
    private static final int CATEGORY_CAP_DIVISOR = 5;


    /**
     * The values this axis actually uses across the catalog, ignoring the literal "All". A product
     * listing every one of them is making the same claim as "All" — see coversEverything().
     */
    private static Set<String> domainOf(List<EngineProduct> products, Function<EngineProduct, List<String>> axis) {
        return products.stream()
                .flatMap(p -> axis.apply(p).stream())
                .filter(v -> !"All".equals(v))
                .collect(Collectors.toSet());
    }

    /**
     * Tagging a product with every destination there is says exactly what "All" says — but scored
     * naively it earns AXIS_MATCH (2) where the honest "All" earns AXIS_ALL (0.5), paying four
     * times more for the same non-claim. Exhaustive tagging shouldn't outrank honesty, so it's
     * treated as neutral either way.
     */
    private static boolean coversEverything(List<String> tags, Set<String> domain) {
        return tags.contains("All") || (!domain.isEmpty() && tags.containsAll(domain));
    }

    /** Tri-state fit for one axis. */
    private static double axisScore(List<String> tags, String answer, Set<String> domain) {
        if (tags.isEmpty() || coversEverything(tags, domain)) return AXIS_ALL;
        if (tags.contains(answer)) return AXIS_MATCH;
        return AXIS_MISMATCH;
    }

    /** Same tri-state fit, but for a multi-select axis (currently just destinations). An empty
     * answer list means the shopper picked "All" — every product is neutral on this axis. */
    private static double multiAxisScore(List<String> tags, List<String> answers, Set<String> domain) {
        if (answers == null || answers.isEmpty()) return AXIS_ALL;
        if (tags.isEmpty() || coversEverything(tags, domain)) return AXIS_ALL;
        if (tags.stream().anyMatch(answers::contains)) return AXIS_MATCH;
        return AXIS_MISMATCH;
    }

    /**
     * The value sets each axis actually uses across the catalog.
     *
     * <p>Public because it belongs to the cached catalog snapshot rather than to a single kit build:
     * it must be derived from the <em>whole</em> catalog. Computed from a filtered subset, the
     * domains shrink and products that legitimately matched one specific value start reading as
     * "covers everything" — measured at 13 of 20 candidates flipping from AXIS_MATCH to AXIS_ALL.
     */
    public record AxisDomains(Set<String> destinations, Set<String> seasons, Set<String> parties) {
        public static AxisDomains of(List<EngineProduct> products) {
            return new AxisDomains(
                    domainOf(products, EngineProduct::destinations),
                    domainOf(products, EngineProduct::seasons),
                    domainOf(products, EngineProduct::parties));
        }
    }

    private static int activityOverlap(EngineProduct product, List<String> selected) {
        if (selected == null || selected.isEmpty() || product.activities().isEmpty()) return 0;
        return (int) product.activities().stream().filter(selected::contains).count();
    }

    /** Soft boost only — null/omitted transportModes means unrestricted; unlike axis tags, this
     * never excludes a product, only nudges it up. */
    private static double transportBoost(EngineProduct product, String answer) {
        if (answer == null || answer.isBlank() || product.transportModes().isEmpty()) return 0;
        return product.transportModes().contains(answer) ? TRIP_SIGNAL_WEIGHT : 0;
    }

    /** Soft boost — compares Duration's stable `code`, which is what KitAnswers carries. */
    private static double durationBoost(EngineProduct product, String answer) {
        if (answer == null || answer.isBlank() || product.durations().isEmpty()) return 0;
        return product.durations().contains(answer) ? TRIP_SIGNAL_WEIGHT : 0;
    }

    /** Soft boost — gender is a display value, not a code (Gender has no stable code). */
    private static double genderBoost(EngineProduct product, String answer) {
        if (answer == null || answer.isBlank() || product.genders().isEmpty()) return 0;
        return product.genders().contains(answer) ? TRIP_SIGNAL_WEIGHT : 0;
    }

    /** See the weight constants above for the grading rule and BASELINE_KIT_CATEGORY for its
     * one exception. */
    private static double kitCategoryBoost(EngineProduct product, List<String> selected) {
        if (selected == null || selected.isEmpty() || product.kitCategories().isEmpty()) return 0;

        long specific = product.kitCategories().stream()
                .filter(c -> !BASELINE_KIT_CATEGORY.equals(c))
                .filter(selected::contains)
                .count();
        // Matching only the baseline is not a reason to pack something.
        if (specific == 0) return 0;

        boolean baselineToo = selected.contains(BASELINE_KIT_CATEGORY)
                && product.kitCategories().contains(BASELINE_KIT_CATEGORY);
        long matches = specific + (baselineToo ? 1 : 0);
        return Math.min(KIT_CATEGORY_WEIGHT + (matches - 1) * KIT_CATEGORY_EXTRA, KIT_CATEGORY_CAP);
    }

    private static int effectivePartySize(KitAnswers a) {
        if (a.partySize() != null) return a.partySize();
        return "Group".equals(a.party()) ? 4 : 1;
    }


    /**
     * unmatched = the product earned nothing: neutral on all three axis tags and no activity,
     * kit-category, transport, trip-length or gender hit. It fits this trip no better
     * than any other, so it isn't recommended — being untagged, or tagged for something the shopper
     * didn't ask for, is not a reason to pack it.
     */
    private record Scored(EngineProduct product, double score, boolean unmatched) {
    }

    private static Scored score(EngineProduct product, KitAnswers a, AxisDomains domains) {
        double d = multiAxisScore(product.destinations(), a.destinations(), domains.destinations());
        double s = axisScore(product.seasons(), a.season(), domains.seasons());
        double p = axisScore(product.parties(), a.party(), domains.parties());
        if (d == AXIS_MISMATCH || s == AXIS_MISMATCH || p == AXIS_MISMATCH) return null; // excluded

        int overlap = activityOverlap(product, a.activities());
        double activity = Math.min(overlap * ACTIVITY_WEIGHT, ACTIVITY_CAP);
        double transport = transportBoost(product, a.transportation());
        double kitCat = kitCategoryBoost(product, a.priorityCategories());
        double duration = durationBoost(product, a.duration());
        double gender = genderBoost(product, a.gender());
        double total = d + s + p + activity + transport + kitCat + duration + gender;
        if (product.popular()) total += 0.8;
        if (product.tested()) total += 0.5;

        boolean unmatched = d == AXIS_ALL && s == AXIS_ALL && p == AXIS_ALL
                && overlap == 0 && transport == 0 && kitCat == 0
                && duration == 0 && gender == 0;
        return new Scored(product, total, unmatched);
    }

    private static int targetSize(KitAnswers a) {
        int base = TARGET_BY_DURATION.getOrDefault(a.duration(), 18);
        int size = effectivePartySize(a);
        int extra = size >= 3 ? Math.min((size - 2) / 2, 4) : 0;
        return Math.max(MIN_KIT, Math.min(MAX_KIT, base + extra));
    }

    /**
     * Does this product match something the shopper actually asked for? Equivalent to
     * kitCategoryBoost() being non-zero, and deliberately so — the baseline kit is excluded here for
     * the same reason it earns nothing there: it's the broadest bucket in the catalog, so matching
     * it is not evidence the shopper wanted this product.
     */
    private static boolean requested(EngineProduct product, List<String> picked) {
        return product.kitCategories().stream()
                .anyMatch(c -> !BASELINE_KIT_CATEGORY.equals(c) && picked.contains(c));
    }

    /**
     * Accumulates the kit, enforcing the two structural rules that scoring alone can't express: the
     * target size, and the per-product-category diversity cap. Kept as a small mutable helper so the
     * selection passes below read as intent ("breadth, then depth, then round it out") rather than
     * as bookkeeping.
     */
    private static final class KitBuilder {
        private final int target;
        private final int categoryCap;
        private final List<Scored> chosen = new ArrayList<>();
        private final Set<String> takenIds = new HashSet<>();
        private final Map<String, Integer> perCategory = new HashMap<>();

        KitBuilder(int target, int categoryCap) {
            this.target = target;
            this.categoryCap = categoryCap;
        }

        int size() {
            return chosen.size();
        }

        boolean full() {
            return chosen.size() >= target;
        }

        /** @return true if the product was added; false if full, already taken, or category-capped. */
        boolean add(Scored x) {
            if (full() || takenIds.contains(x.product().id())) return false;
            String category = x.product().category() != null ? x.product().category() : "";
            if (perCategory.getOrDefault(category, 0) >= categoryCap) return false;
            takenIds.add(x.product().id());
            perCategory.merge(category, 1, Integer::sum);
            chosen.add(x);
            return true;
        }

        /** Take from `pool` in score order until the kit reaches `limit` picks (or the pool runs out). */
        void fill(List<Scored> pool, int limit) {
            for (Scored x : pool) {
                if (chosen.size() >= limit || full()) return;
                add(x);
            }
        }

        List<Scored> chosen() {
            return chosen;
        }
    }

    /**
     * Build a ranked kit. Selection runs in tiers rather than one flat ranking: what the shopper
     * asked for fills the kit first, and merely-related items round it out without taking it over.
     *
     * <p>Tiering exists because a single additive score let an incidental tag outbid an explicit
     * request — a swimsuit matching the selected activity outscored the toiletry kit that was
     * actually asked for. No weight tuning fixes that reliably; separating demand from relevance
     * does.
     */
    public static List<KitPick> buildKit(KitAnswers answers, List<EngineProduct> products) {
        return buildKit(answers, products, AxisDomains.of(products));
    }

    /**
     * As above, with the axis domains supplied rather than derived — the caller holds them because
     * they describe the whole catalog and only change when it does. See {@link AxisDomains}.
     */
    public static List<KitPick> buildKit(KitAnswers answers, List<EngineProduct> products, AxisDomains domains) {
        List<Scored> eligible = products.stream()
                .map(p -> score(p, answers, domains))
                .filter(Objects::nonNull)
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();

        // Only products that earned something. A kit is allowed to come back shorter than the
        // target rather than padded with items that fit this trip no better than any other.
        List<Scored> specifics = eligible.stream().filter(x -> !x.unmatched()).toList();

        List<String> picked = answers.priorityCategories() != null ? answers.priorityCategories() : List.of();
        Map<Boolean, List<Scored>> byTier = specifics.stream()
                .collect(Collectors.partitioningBy(x -> requested(x.product(), picked)));
        List<Scored> requested = byTier.get(true);
        List<Scored> related = byTier.get(false);

        int target = targetSize(answers);
        KitBuilder kit = new KitBuilder(target, Math.max(MIN_CATEGORY_CAP, target / CATEGORY_CAP_DIVISOR));

        // 1 — breadth: the best still-available item for each kit the shopper picked, so the kit
        // spans what they asked for instead of burying the narrow kits under the broad ones.
        for (String pickedKit : picked) {
            for (Scored x : requested) {
                if (kit.full()) break;
                if (x.product().kitCategories().contains(pickedKit) && kit.add(x)) break;
            }
        }

        // 2 — depth: fill out the requested kits by score.
        kit.fill(requested, target);

        // 3 — round it out with related items, held to a minority share of the finished kit. When
        // nothing was requested — kit category is Optional, or nothing in the catalog matched what
        // was picked — there's nothing to be a minority of, so the whole target is theirs.
        int fromRequested = kit.size();
        int relatedLimit = fromRequested == 0
                ? target
                : fromRequested + Math.max(MIN_RELATED,
                        (int) Math.floor(fromRequested * RELATED_SHARE / (1 - RELATED_SHARE)));
        kit.fill(related, Math.min(target, relatedLimit));

        // Requested items lead, each block by score. The shopper should see what they asked for
        // first; a flat score sort buried it among the extras, which reads as not having listened.
        return kit.chosen().stream()
                .sorted(Comparator.comparing((Scored x) -> requested(x.product(), picked) ? 0 : 1)
                        .thenComparing(x -> -x.score()))
                .map(x -> new KitPick(x.product().name(), x.product().id(), x.product().category()))
                .collect(Collectors.toList());
    }
}
