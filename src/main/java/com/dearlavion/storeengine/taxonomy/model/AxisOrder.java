package com.dearlavion.storeengine.taxonomy.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/** Single-document singleton (_id="axis_order") holding the admin-configurable order of the 8
 * taxonomy axes — drives both the Kit Settings page's section order and the /travel survey's step
 * order, so the two always agree. */
@Getter
@Setter
@Document(collection = "kit_builder_axis_order")
public class AxisOrder {

    public static final String SINGLETON_ID = "axis_order";

    public static final List<String> DEFAULT_ORDER = List.of(
            "destination", "season", "duration", "party", "transportation", "activity", "kitCategory", "gender"
    );

    @Id
    private String id = SINGLETON_ID;

    private List<String> order = DEFAULT_ORDER;
}
