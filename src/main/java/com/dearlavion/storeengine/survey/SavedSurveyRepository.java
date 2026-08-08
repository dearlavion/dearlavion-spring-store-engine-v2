package com.dearlavion.storeengine.survey;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SavedSurveyRepository extends MongoRepository<SavedSurvey, String> {
    List<SavedSurvey> findByUserIdOrderByCreatedAtDesc(String userId);

    void deleteByIdAndUserId(String id, String userId);
}
