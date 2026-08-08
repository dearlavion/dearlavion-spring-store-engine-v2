package com.dearlavion.storeengine.survey;

import com.dearlavion.storeengine.survey.request.SurveyAnswersRequest;
import com.dearlavion.storeengine.survey.response.RecommendationsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Public: build the checklist + matching store products for a set of survey answers. */
@RestController
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService service;

    @PostMapping("/survey/recommendations")
    public RecommendationsResponse recommendations(@Valid @RequestBody SurveyAnswersRequest answers) {
        return service.recommendations(answers);
    }
}
