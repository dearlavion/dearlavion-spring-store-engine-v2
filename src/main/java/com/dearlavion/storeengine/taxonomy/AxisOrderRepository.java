package com.dearlavion.storeengine.taxonomy;

import com.dearlavion.storeengine.taxonomy.model.AxisOrder;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AxisOrderRepository extends MongoRepository<AxisOrder, String> {
}
