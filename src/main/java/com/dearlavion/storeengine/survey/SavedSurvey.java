package com.dearlavion.storeengine.survey;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A user's saved survey result (their answers + the generated checklist + recommended products). */
@Getter
@Setter
@Document(collection = "saved_surveys")
public class SavedSurvey {

    @Id
    private String id;

    @Indexed
    private String userId;

    private SurveyAnswersEmbedded answers;

    private List<String> checklist = new ArrayList<>();

    private List<String> recommendedProductIds = new ArrayList<>();

    private Instant createdAt;
}
