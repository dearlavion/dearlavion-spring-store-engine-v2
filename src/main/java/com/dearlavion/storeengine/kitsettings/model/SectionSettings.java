package com.dearlavion.storeengine.kitsettings.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * How one collection behaves as a question in the /travel survey — set per section on the admin Kit
 * Settings page, alongside its position in the order.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SectionSettings {

    /** false = the shopper may skip this question (today: activity and gender). */
    private boolean required = true;

    /** true = the question accepts several answers (today: destination, activity, kitCategory). */
    private boolean multiple = false;
}
