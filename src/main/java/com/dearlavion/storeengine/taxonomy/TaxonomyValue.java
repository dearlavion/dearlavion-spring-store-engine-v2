package com.dearlavion.storeengine.taxonomy;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** One admin-editable option within one of the 8 axes that drive the /travel survey and product
 * tagging: destination | season | party | transportation | activity | kitCategory | duration | gender. */
@Getter
@Setter
@Document(collection = "kit_builder_settings")
@CompoundIndex(name = "axis_value_unique", def = "{'axis': 1, 'value': 1}", unique = true)
public class TaxonomyValue {

    @Id
    private String id;

    @Indexed
    private String axis;

    /** Editable display label, e.g. "Beach". For every axis except 'duration', this value itself is
     * also the key stored on tagged Products/survey answers — renaming it does not retroactively
     * update already-tagged data (same trade-off the existing Category collection already has). */
    private String value;

    /** Display/sort order within its axis. */
    private int order;

    /** Optional — Shop's filter chips display these today. */
    private String emoji;

    /** Optional helper text — only 'duration' uses this today (e.g. "2-4 days"). */
    private String subtext;

    /** Only set for axis:'duration' ('short'|'medium'|'long') — the stable scoring key kit-engine
     * keys its kit-size targets on, kept separate from `value` so an admin can freely rename
     * Duration's display labels without breaking that lookup. Every other axis leaves this unset. */
    private String code;
}
