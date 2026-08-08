package com.dearlavion.storeengine.productitem;

import com.dearlavion.storeengine.productitem.model.ProductItem;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductItemRepository extends MongoRepository<ProductItem, String> {
    List<ProductItem> findByProductId(String productId, Sort sort);

    List<ProductItem> findByProductIdAndActiveTrue(String productId, Sort sort);

    List<ProductItem> findByProductIdInAndActiveTrue(List<String> productIds, Sort sort);

    List<ProductItem> findByActiveTrue();
}
