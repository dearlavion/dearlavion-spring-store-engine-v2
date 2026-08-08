package com.dearlavion.storeengine.survey.response;

import com.dearlavion.storeengine.product.model.Product;
import com.dearlavion.storeengine.survey.model.KitChecklistItem;
import com.dearlavion.storeengine.survey.request.SurveyAnswersRequest;

import java.util.List;

public record RecommendationsResponse(SurveyAnswersRequest answers, List<KitChecklistItem> checklist, List<Product> products) {
}
