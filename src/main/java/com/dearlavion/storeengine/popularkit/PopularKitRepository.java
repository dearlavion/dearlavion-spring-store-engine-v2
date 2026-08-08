package com.dearlavion.storeengine.popularkit;

import com.dearlavion.storeengine.popularkit.model.PopularKit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PopularKitRepository extends MongoRepository<PopularKit, String> {
    List<PopularKit> findByActiveTrueOrderByCreatedAtAsc();

    List<PopularKit> findAllByOrderByCreatedAtAsc();

    Optional<PopularKit> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
