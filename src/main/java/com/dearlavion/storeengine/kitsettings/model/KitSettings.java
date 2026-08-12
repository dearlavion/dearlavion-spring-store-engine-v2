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

    public static final String SINGLETON_ID = "kit_settings";

    public static final List<String> DEFAULT_ORDER = List.of(
            "destination", "season", "duration", "party", "transportation", "activity", "kitCategory", "gender"
    );

    /**
     * Mirrors how the survey behaves today, so defaulting changes nothing until an admin edits it:
     * destination/activity/kitCategory take several answers, and activity/gender may be skipped.
     */
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
    private String id = SINGLETON_ID;

    private List<String> order = DEFAULT_ORDER;

    /** Keyed by collection key. A key with no entry falls back to required + single-select. */
    private Map<String, SectionSettings> sections = defaultSections();
}
