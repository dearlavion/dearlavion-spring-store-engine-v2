package com.dearlavion.storeengine.collection;

import com.dearlavion.storeengine.collection.model.SavedKit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SavedKitRepository extends MongoRepository<SavedKit, String> {
    List<SavedKit> findByUserIdOrderBySavedAtDesc(String userId);

    Optional<SavedKit> findByIdAndUserId(String id, String userId);

    long deleteByIdAndUserId(String id, String userId);
}
