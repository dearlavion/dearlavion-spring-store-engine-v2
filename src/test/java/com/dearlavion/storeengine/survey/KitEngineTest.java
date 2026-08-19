package com.dearlavion.storeengine.survey;

import com.dearlavion.storeengine.survey.model.EngineProduct;
import com.dearlavion.storeengine.survey.model.KitAnswers;
import com.dearlavion.storeengine.survey.model.KitPick;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the matching rules. `buildKit` is pure and static, so these need no Spring context — which
 * matters, because these rules were previously only verifiable by running a survey against the live
 * catalog and eyeballing the result.
 *
 * <p>Each test names the decision it protects rather than the mechanics, so a future change that
 * breaks one says which product rule it broke.
 */
class KitEngineTest {

    private static EngineProduct product(String id, List<String> destinations, List<String> seasons,
                                         List<String> parties, List<String> activities, List<String> kitCategories) {
        // Each product gets its own product category by default: the diversity cap is per-category,
        // so a shared one would silently cap unrelated tests at 3 items.
        return product(id, "category-" + id, destinations, seasons, parties, activities, kitCategories);
    }

    private static EngineProduct product(String id, String category, List<String> destinations, List<String> seasons,
                                         List<String> parties, List<String> activities, List<String> kitCategories) {
        return new EngineProduct(id, id, category, destinations, seasons, parties, activities,
                List.of(), List.of(), List.of(), kitCategories, false, false);
    }

    private static KitAnswers answers(List<String> destinations, String season, String party,
                                      String duration, List<String> activities, List<String> kits) {
        return new KitAnswers(destinations, season, party, null, duration, activities, null, null, kits);
    }

    private static List<String> idsOf(List<KitPick> picks) {
        return picks.stream().map(KitPick::productId).toList();
    }

    @Test
    void excludesProductsTaggedForOtherSeasons() {
        EngineProduct summerOnly = product("summer", List.of("All"), List.of("Summer"), List.of("All"), List.of("Hiking"), List.of("Weather Kit"));
        EngineProduct rainy = product("rainy", List.of("All"), List.of("Rainy"), List.of("All"), List.of("Hiking"), List.of("Weather Kit"));

        List<KitPick> kit = KitEngine.buildKit(
                answers(List.of("Mountain"), "Rainy", "Solo", "day", List.of("Hiking"), List.of("Weather Kit")),
                List.of(summerOnly, rainy));

        assertThat(idsOf(kit)).containsExactly("rainy");
    }

    @Test
    void doesNotRecommendAProductThatMatchedNothing() {
        // Neutral everywhere and tagged for kits nobody asked for: it fits this trip no better than
        // any other, so it isn't packed even though slots remain.
        EngineProduct generic = product("generic", List.of("All"), List.of("All"), List.of("All"), List.of(), List.of("Laundry Kit"));
        EngineProduct relevant = product("relevant", List.of("Mountain"), List.of("All"), List.of("All"), List.of("Hiking"), List.of("Weather Kit"));

        List<KitPick> kit = KitEngine.buildKit(
                answers(List.of("Mountain"), "Rainy", "Solo", "day", List.of("Hiking"), List.of("Weather Kit")),
                List.of(generic, relevant));

        assertThat(idsOf(kit)).containsExactly("relevant");
    }

    @Test
    void essentialsKitAloneIsNeverEnough() {
        EngineProduct essentialsOnly = product("essentials-only", List.of("All"), List.of("All"), List.of("All"), List.of(), List.of("Essentials Kit", "Laundry Kit"));

        List<KitPick> kit = KitEngine.buildKit(
                answers(List.of("Mountain"), "Rainy", "Solo", "day", List.of(), List.of("Essentials Kit", "Weather Kit")),
                List.of(essentialsOnly));

        assertThat(idsOf(kit)).isEmpty();
    }

    @Test
    void essentialsKitCountsOnceAnotherPickedKitMatches() {
        EngineProduct laundry = product("laundry", List.of("All"), List.of("All"), List.of("All"), List.of(), List.of("Essentials Kit", "Laundry Kit"));

        List<KitPick> kit = KitEngine.buildKit(
                answers(List.of("Mountain"), "Rainy", "Solo", "day", List.of(), List.of("Essentials Kit", "Laundry Kit")),
                List.of(laundry));

        assertThat(idsOf(kit)).containsExactly("laundry");
    }

    @Test
    void listingEveryDestinationScoresTheSameAsAll() {
        // Both claim "suits anywhere"; the exhaustive one must not out-rank the honest one.
        EngineProduct exhaustive = product("exhaustive", List.of("Mountain", "Beach", "City"), List.of("All"), List.of("All"), List.of("Hiking"), List.of("Weather Kit"));
        EngineProduct honest = product("honest", List.of("All"), List.of("All"), List.of("All"), List.of("Hiking"), List.of("Weather Kit"));
        EngineProduct specific = product("specific", List.of("Mountain"), List.of("All"), List.of("All"), List.of("Hiking"), List.of("Weather Kit"));

        List<KitPick> kit = KitEngine.buildKit(
                answers(List.of("Mountain"), "Rainy", "Solo", "day", List.of("Hiking"), List.of("Weather Kit")),
                List.of(exhaustive, honest, specific));

        // The genuinely Mountain-specific product leads; the other two tie behind it.
        assertThat(idsOf(kit)).first().isEqualTo("specific");
    }

    @Test
    void breadthSpansThePickedKitsNotTheShopsCategories() {
        // Two products in the same picked kit, one in another: the second kit gets a slot before the
        // first kit gets a second item.
        EngineProduct toiletryBest = product("toiletry-best", List.of("Mountain"), List.of("All"), List.of("Solo"), List.of("Hiking"), List.of("Toiletry Kit"));
        EngineProduct toiletryAlso = product("toiletry-also", List.of("Mountain"), List.of("All"), List.of("All"), List.of("Hiking"), List.of("Toiletry Kit"));
        EngineProduct weather = product("weather", List.of("All"), List.of("All"), List.of("All"), List.of(), List.of("Weather Kit"));

        List<KitPick> kit = KitEngine.buildKit(
                answers(List.of("Mountain"), "Rainy", "Solo", "day", List.of("Hiking"), List.of("Toiletry Kit", "Weather Kit")),
                List.of(toiletryBest, toiletryAlso, weather));

        assertThat(idsOf(kit)).contains("toiletry-best", "weather");
    }

    @Test
    void tripLengthCapsTheKitSize() {
        // Distinct product categories throughout: this test is about trip length, not the
        // per-category diversity cap, which would otherwise bind first.
        List<EngineProduct> many = java.util.stream.IntStream.range(0, 40)
                .mapToObj(i -> product("p" + i, "cat" + i, List.of("Mountain"), List.of("All"), List.of("All"),
                        List.of("Hiking"), List.of("Weather Kit")))
                .toList();

        List<KitPick> dayKit = KitEngine.buildKit(
                answers(List.of("Mountain"), "Rainy", "Solo", "day", List.of("Hiking"), List.of("Weather Kit")), many);
        List<KitPick> longKit = KitEngine.buildKit(
                answers(List.of("Mountain"), "Rainy", "Solo", "long", List.of("Hiking"), List.of("Weather Kit")), many);

        assertThat(dayKit).hasSize(10);
        assertThat(longKit).hasSize(26);
    }

    @Test
    void aLongTripFavoursProductsTaggedForLongTrips() {
        // Replaces the old name-regex nudge: "packing cubes are for long trips" is now a `durations`
        // tag, which the admin can see and edit, rather than a word match on the product name.
        EngineProduct taggedForLong = new EngineProduct("tagged", "Compression Cubes", "Category",
                List.of("All"), List.of("All"), List.of("All"), List.of(), List.of(), List.of("long"), List.of(),
                List.of("Weather Kit"), false, false);
        EngineProduct untagged = new EngineProduct("untagged", "Packing Cubes", "Category",
                List.of("All"), List.of("All"), List.of("All"), List.of(), List.of(), List.of(), List.of(),
                List.of("Weather Kit"), false, false);

        List<KitPick> kit = KitEngine.buildKit(
                answers(List.of("Mountain"), "Rainy", "Solo", "long", List.of(), List.of("Weather Kit")),
                List.of(untagged, taggedForLong));

        assertThat(idsOf(kit)).first().isEqualTo("tagged");
    }

    @Test
    void whatTheShopperAskedForOutranksAnIncidentalTagMatch() {
        // The bug this tiering was built for: the swimsuit matches the selected activity and the
        // destination, so it outscored the toiletry kit that was actually requested.
        EngineProduct requested = product("requested", List.of("All"), List.of("All"), List.of("All"),
                List.of("All"), List.of("Toiletry Kit"));
        EngineProduct incidental = product("incidental", List.of("Beach"), List.of("Summer"), List.of("All"),
                List.of("Swimming"), List.of("Activity Gear Kit"));

        List<KitPick> kit = KitEngine.buildKit(
                answers(List.of("Beach"), "Summer", "Solo", "long", List.of("Swimming"), List.of("Toiletry Kit")),
                List.of(incidental, requested));

        assertThat(idsOf(kit)).containsExactly("requested", "incidental"); // requested tier leads
    }

    @Test
    void relatedItemsCannotTakeOverTheKit() {
        // Five requested products against a flood of merely-related ones. The related tier is a
        // share of the kit as built, so it stays a minority however many candidates it has.
        List<EngineProduct> catalog = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            catalog.add(product("requested" + i, "reqcat" + i, List.of("All"), List.of("All"), List.of("All"),
                    List.of(), List.of("Toiletry Kit")));
        }
        for (int i = 0; i < 30; i++) {
            catalog.add(product("related" + i, "relcat" + i, List.of("Beach"), List.of("All"), List.of("All"),
                    List.of("Swimming"), List.of("Activity Gear Kit")));
        }

        List<KitPick> kit = KitEngine.buildKit(
                answers(List.of("Beach"), "Summer", "Solo", "long", List.of("Swimming"), List.of("Toiletry Kit")),
                catalog);

        long requested = kit.stream().filter(p -> p.productId().startsWith("requested")).count();
        long related = kit.size() - requested;
        assertThat(requested).isEqualTo(5);
        assertThat(related).isLessThan(requested); // 5 requested -> floor(5 * 0.4/0.6) = 3
        // ...and the trip being "long" (target 26) doesn't licence padding the other 18 slots.
        assertThat(kit).hasSize(8);
    }

    @Test
    void aThinlyStockedKitStillGetsAFewRelatedItems() {
        // The degenerate case of the share rule: one requested product earns a budget of zero, so a
        // small floor applies instead of returning a kit of one.
        List<EngineProduct> catalog = new java.util.ArrayList<>();
        catalog.add(product("requested", List.of("All"), List.of("All"), List.of("All"), List.of(), List.of("Toiletry Kit")));
        for (int i = 0; i < 10; i++) {
            catalog.add(product("related" + i, "relcat" + i, List.of("Beach"), List.of("All"), List.of("All"),
                    List.of("Swimming"), List.of("Activity Gear Kit")));
        }

        List<KitPick> kit = KitEngine.buildKit(
                answers(List.of("Beach"), "Summer", "Solo", "day", List.of("Swimming"), List.of("Toiletry Kit")),
                catalog);

        assertThat(kit).hasSize(3); // 1 requested + MIN_RELATED
        assertThat(idsOf(kit)).first().isEqualTo("requested"); // requested still leads
    }

    @Test
    void withNoKitsPickedTheRelatedCapDoesNotApply() {
        // Kit category is Optional in Kit Settings. With nothing picked there is nothing to relate
        // to, so capping the "related" tier would shrink every kit for no reason.
        List<EngineProduct> catalog = java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> product("p" + i, "cat" + i, List.of("Beach"), List.of("All"), List.of("All"),
                        List.of("Swimming"), List.of("Activity Gear Kit")))
                .toList();

        List<KitPick> kit = KitEngine.buildKit(
                answers(List.of("Beach"), "Summer", "Solo", "day", List.of("Swimming"), List.of()),
                catalog);

        assertThat(kit).hasSize(10); // the full day-trip target, not 40% of it
    }

    @Test
    void oneProductCategoryCannotDominateTheKit() {
        // Five near-identical travel accessories was a real result; the cap is 3 on a day trip.
        List<EngineProduct> catalog = java.util.stream.IntStream.range(0, 12)
                .mapToObj(i -> product("acc" + i, "Travel Accessories", List.of("All"), List.of("All"),
                        List.of("All"), List.of(), List.of("Toiletry Kit")))
                .toList();

        List<KitPick> kit = KitEngine.buildKit(
                answers(List.of("Beach"), "Summer", "Solo", "day", List.of(), List.of("Toiletry Kit")),
                catalog);

        assertThat(kit).hasSize(3);
    }

    @Test
    void activityTaggingBreadthCannotOutbidAnExplicitRequest() {
        // Uncapped, three overlapping activities scored +9 and beat a kit request's +5. Capped at 6
        // the explicit ask stays ahead. Both are in the requested tier here, so this is purely
        // about the within-tier score.
        EngineProduct manyActivities = product("many-activities", List.of("All"), List.of("All"), List.of("All"),
                List.of("Hiking", "Swimming", "Sightseeing"), List.of("Toiletry Kit"));
        EngineProduct twoKits = product("two-kits", List.of("All"), List.of("All"), List.of("All"),
                List.of(), List.of("Toiletry Kit", "Weather Kit"));

        List<KitPick> kit = KitEngine.buildKit(
                answers(List.of("Beach"), "Summer", "Solo", "day",
                        List.of("Hiking", "Swimming", "Sightseeing"), List.of("Toiletry Kit", "Weather Kit")),
                List.of(manyActivities, twoKits));

        // two-kits: 1.5 axes + 6.5 kit = 8. many-activities: 1.5 axes + 6 capped activity + 5 kit = 12.5.
        // The capped activity score still wins here, but only because it also matches a picked kit —
        // what the cap prevents is activity breadth alone carrying a product past a kit request.
        assertThat(idsOf(kit)).containsExactly("many-activities", "two-kits");
    }
}
