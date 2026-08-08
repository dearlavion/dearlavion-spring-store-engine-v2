package com.dearlavion.storeengine.category;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findAllByOrderByNameAsc();

    Optional<Category> findBySlug(String slug);
}
