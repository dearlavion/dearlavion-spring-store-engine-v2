package com.dearlavion.storeengine.survey;

import com.dearlavion.storeengine.product.ProductService;
import com.dearlavion.storeengine.product.model.Product;
import com.dearlavion.storeengine.productitem.ProductItemService;
import com.dearlavion.storeengine.productitem.model.ProductItem;
import com.dearlavion.storeengine.survey.model.EngineProduct;
import com.dearlavion.storeengine.survey.model.KitAnswers;
import com.dearlavion.storeengine.survey.model.KitChecklistItem;
import com.dearlavion.storeengine.survey.model.KitPick;
import com.dearlavion.storeengine.survey.model.SavedSurvey;
import com.dearlavion.storeengine.survey.model.SurveyAnswersEmbedded;
import com.dearlavion.storeengine.survey.request.SurveyAnswersRequest;
import com.dearlavion.storeengine.survey.response.RecommendationsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SurveyService {

    // Keyed on Duration's stable `code`, not its admin-editable display label — see KitEngine.
    private static final Map<String, Integer> DURATION_TIER = Map.of("short", 1, "medium", 2, "long", 3);

    private final SavedSurveyRepository repository;
    private final ProductService products;
    private final ProductItemService productItems;

    private static EngineProduct toEngineProduct(Product p) {
        return new EngineProduct(
                p.getId(),
                p.getName(),
                p.getCategory(),
                p.getDestinations() != null ? p.getDestinations() : List.of(),
                p.getSeasons() != null ? p.getSeasons() : List.of(),
                p.getParties() != null ? p.getParties() : List.of(),
                p.getActivities() != null ? p.getActivities() : List.of(),
                p.getTransportModes() != null ? p.getTransportModes() : List.of(),
                p.getKitCategory(),
                p.isPopular(),
                p.isTested()
        );
    }

    /** The size tier this trip wants: longer trips + bigger groups -> larger sizes (1-3). */
    private static int desiredSizeTier(SurveyAnswersRequest a) {
        int base = DURATION_TIER.getOrDefault(a.duration(), 2);
        int size = a.partySize() != null ? a.partySize() : ("Group".equals(a.party()) ? 4 : 1);
        int bump = size >= 4 ? (size - 1) / 3 : 0; // 4-6 -> +1, 7-9 -> +2, ...
        return Math.max(1, Math.min(3, base + bump));
    }

    /** Choose the item whose sizeTier is nearest the trip's desired tier; if none are sized, the
     * cheapest (items arrive cheapest-first). Ties break toward the smaller size. */
    private static ProductItem pickSizedItem(List<ProductItem> items, int tier) {
        List<ProductItem> sized = items.stream().filter(i -> i.getSizeTier() != null).toList();
        if (sized.isEmpty()) return items.isEmpty() ? null : items.get(0);
        ProductItem best = sized.get(0);
        for (ProductItem i : sized) {
            int di = Math.abs(i.getSizeTier() - tier);
            int db = Math.abs(best.getSizeTier() - tier);
            if (di < db || (di == db && i.getSizeTier() < best.getSizeTier())) best = i;
        }
        return best;
    }

    private record RunEngineResult(List<KitChecklistItem> checklist, List<Product> products) {
    }

    /** Score the catalog, then resolve each pick to the trip-appropriate SKU (size). */
    private RunEngineResult runEngine(SurveyAnswersRequest answers) {
        List<Product> active = products.allActive();
        KitAnswers kitAnswers = new KitAnswers(
                answers.destinations() != null ? answers.destinations() : List.of(),
                answers.season(), answers.party(), answers.partySize(), answers.duration(),
                answers.activities(), answers.transportation(), answers.priorityCategories()
        );
        List<EngineProduct> engineProducts = active.stream().map(SurveyService::toEngineProduct).toList();
        List<KitPick> picks = KitEngine.buildKit(kitAnswers, engineProducts);
        Map<String, Product> byId = new HashMap<>();
        for (Product p : active) byId.put(p.getId(), p);
        List<Product> matchedProducts = picks.stream().map(pk -> byId.get(pk.productId())).filter(Objects::nonNull).toList();

        // Resolve the right size per product for this trip.
        int tier = desiredSizeTier(answers);
        List<String> productIds = picks.stream().map(KitPick::productId).toList();
        List<ProductItem> items = productItems.activeForProducts(productIds);
        Map<String, List<ProductItem>> itemsByProduct = new HashMap<>();
        for (ProductItem it : items) {
            itemsByProduct.computeIfAbsent(it.getProductId(), k -> new ArrayList<>()).add(it);
        }

        List<KitChecklistItem> checklist = picks.stream().map(pk -> {
            ProductItem item = pickSizedItem(itemsByProduct.getOrDefault(pk.productId(), List.of()), tier);
            boolean hasSizeLabel = item != null && item.getSizeLabel() != null && !item.getSizeLabel().isBlank();
            String label = hasSizeLabel ? pk.label() + " (" + item.getSizeLabel() + ")" : pk.label();
            return new KitChecklistItem(label, pk.productId(), item != null ? item.getId() : null,
                    item != null ? item.getSizeLabel() : null);
        }).toList();

        return new RunEngineResult(checklist, matchedProducts);
    }

    /** The "free tool -> store suggestions" endpoint: scores the catalog against the survey
     * answers and returns the ranked packing checklist (size-matched to the trip) + matching
     * store products. */
    public RecommendationsResponse recommendations(SurveyAnswersRequest answers) {
        RunEngineResult r = runEngine(answers);
        return new RecommendationsResponse(answers, r.checklist(), r.products());
    }

    /** Save a survey result for the logged-in user (persists answers + checklist + product ids). */
    public SavedSurvey save(String userId, SurveyAnswersRequest answers) {
        RunEngineResult r = runEngine(answers);
        SavedSurvey survey = new SavedSurvey();
        survey.setUserId(userId);
        SurveyAnswersEmbedded embedded = new SurveyAnswersEmbedded();
        embedded.setDestinations(answers.destinations() != null ? answers.destinations() : List.of());
        embedded.setSeason(answers.season());
        embedded.setParty(answers.party());
        embedded.setPartySize(answers.partySize() != null ? answers.partySize() : 1);
        embedded.setDuration(answers.duration());
        survey.setAnswers(embedded);
        survey.setChecklist(r.checklist().stream().map(KitChecklistItem::label).toList());
        survey.setRecommendedProductIds(r.products().stream().map(Product::getId).toList());
        survey.setCreatedAt(Instant.now());
        return repository.save(survey);
    }

    public List<SavedSurvey> listForUser(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void delete(String userId, String id) {
        try {
            repository.deleteByIdAndUserId(id, userId);
        } catch (Exception ignored) {
            // matches NestJS's .catch(() => undefined) — a malformed id is a silent no-op.
        }
    }
}
