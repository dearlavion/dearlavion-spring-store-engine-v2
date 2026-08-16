package com.dearlavion.storeengine.kitsettings.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single-document singleton ({@code _id: "kit_settings"}) holding everything the admin Kit Settings
 * page configures: the order the /travel survey asks its questions in, and how each question
 * behaves (optional/required, single/multiple).
 *
 * <p>Owned by this service because it configures the survey KitEngine/SurveyService run. The
 * option lists it references (destination, season, …) are owned by
 * dearlavion-spring-master-data-service; only their keys appear here.
 */
@Getter
@Setter
@Document(collection = "kit_settings")
public class KitSettings {

    /** The survey's questions: what /travel asks, in what order, and how each is answered. */
    public static final String SURVEY_ID = "survey_kit_settings";
    /** The admin product form's fields: which collections a product can be tagged with. */
    public static final String PRODUCT_ID = "product_kit_settings";
    /** Pre-split document, kept for one release as the rollback path — see KitSettingsMigration. */
    public static final String LEGACY_ID = "kit_settings";

    public static final List<String> DEFAULT_ORDER = List.of(
            "destination", "season", "duration", "party", "transportation", "activity", "kitCategory", "gender"
    );

    /**
     * Mirrors how the survey behaves today, so defaulting changes nothing until an admin edits it:
     * destination/activity/kitCategory take several answers, and activity/gender may be skipped.
     */
    /**
     * The product form's own defaults. Deliberately NOT the survey's: a shopper picks one trip
     * length and one gender, but a product suits several of each (`durations`/`genders` are
     * arrays), so copying the survey's shape renders those fields single-select and silently
     * discards the admin's earlier pick.
     */
    public static final List<String> DEFAULT_PRODUCT_ORDER = List.of(
            "productCategory", "kitCategory", "destination", "season",
            "duration", "party", "transportation", "activity", "gender"
    );

    public static Map<String, SectionSettings> defaultProductSections() {
        Map<String, SectionSettings> defaults = new LinkedHashMap<>();
        // Both required: save() rejects a product without them.
        defaults.put("productCategory", new SectionSettings(true, false));
        defaults.put("kitCategory", new SectionSettings(true, true));
        // Every other axis is a multi-value tag on the product, and empty means "suits all".
        for (String key : List.of("destination", "season", "duration", "party", "transportation", "activity", "gender")) {
            defaults.put(key, new SectionSettings(false, true));
        }
        return defaults;
    }

    public static Map<String, SectionSettings> defaultSections() {
        Map<String, SectionSettings> defaults = new LinkedHashMap<>();
        defaults.put("destination", new SectionSettings(true, true));
        defaults.put("season", new SectionSettings(true, false));
        defaults.put("duration", new SectionSettings(true, false));
        defaults.put("party", new SectionSettings(true, false));
        defaults.put("transportation", new SectionSettings(true, false));
        defaults.put("activity", new SectionSettings(false, true));
        defaults.put("kitCategory", new SectionSettings(true, true));
        defaults.put("gender", new SectionSettings(false, false));
        return defaults;
    }

    @Id
    private String id = SURVEY_ID;

    private List<String> order = DEFAULT_ORDER;

    /** Keyed by collection key. A key with no entry falls back to required + single-select. */
    private Map<String, SectionSettings> sections = defaultSections();
}
