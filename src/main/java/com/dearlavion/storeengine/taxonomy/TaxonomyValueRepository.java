package com.dearlavion.storeengine.taxonomy;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TaxonomyValueRepository extends MongoRepository<TaxonomyValue, String> {
    List<TaxonomyValue> findAllByOrderByAxisAscOrderAsc();

    Optional<TaxonomyValue> findByAxisAndValue(String axis, String value);
}
