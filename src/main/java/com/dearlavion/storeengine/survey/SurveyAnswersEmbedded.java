package com.dearlavion.storeengine.survey;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Persisted subset of SurveyAnswersRequest — matches survey.schema.ts's SurveyAnswers exactly
 * (activities/transportation/priorityCategories are transient scoring inputs, not persisted). */
@Getter
@Setter
public class SurveyAnswersEmbedded {
    private List<String> destinations = new ArrayList<>();
    private String season;
    private String party;
    private int partySize = 1;
    private String duration;
}
