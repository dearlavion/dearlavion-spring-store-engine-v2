package com.dearlavion.storeengine.product;

import com.dearlavion.storeengine.product.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
}
