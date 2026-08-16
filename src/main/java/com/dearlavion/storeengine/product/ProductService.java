package com.dearlavion.storeengine.product;

import com.dearlavion.storeengine.common.PageResponse;
import com.dearlavion.storeengine.common.Slugify;
import com.dearlavion.storeengine.common.exception.NotFoundException;
import com.dearlavion.storeengine.product.model.Product;
import com.dearlavion.storeengine.product.model.ProductFilter;
import com.dearlavion.storeengine.product.request.CreateProductRequest;
import com.dearlavion.storeengine.product.request.UpdateProductRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import com.dearlavion.storeengine.productitem.ProductItemService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final MongoTemplate mongoTemplate;
    private final ProductMapper mapper;
    // Lazy: ProductItemService reads products, so constructor injection either way
    // round would be a cycle.
    private final ObjectProvider<ProductItemService> productItems;

    public PageResponse<Product> list(ProductFilter f) {
        int page = f.page() != null ? f.page() : 0;
        int size = f.size() != null ? f.size() : 100;
        Query query = ProductQueryBuilder.buildQuery(f);
        long total = mongoTemplate.count(query, Product.class);
        query.skip((long) page * size).limit(size);
        List<Product> content = mongoTemplate.find(query, Product.class);
        return new PageResponse<>(content, page, size, total);
    }

    /** Look up an active product by id (public product detail) — id is already the slug. */
    public Product getById(String id) {
        return repository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    /** Products matching survey tags (destination/season/party) — used by recommendations. */
    public List<Product> matchForSurvey(String destination, String season, String party) {
        ProductFilter f = new ProductFilter(destination, season, party, null, null, null, null, null, false);
        return mongoTemplate.find(ProductQueryBuilder.buildQuery(f), Product.class);
    }

    /** All active products — the recommendation engine scores/filters these in memory. */
    public List<Product> allActive() {
        return mongoTemplate.find(Query.query(Criteria.where("active").is(true)), Product.class);
    }

    // ---- admin ----

    public Product create(CreateProductRequest dto) {
        return repository.save(mapper.toEntity(dto, uniqueId(dto.name())));
    }

    /** id is fixed at creation and never changes here, even when dto.name() renames the product —
     * avoids invalidating every existing reference to this product (ProductItem.productId,
     * PopularKit/Product.linkedProductIds, saved carts/kits) on a simple rename. */
    public Product update(String id, UpdateProductRequest dto) {
        Product product = requireById(id);
        mapper.applyPatch(product, dto);
        return repository.save(product);
    }

    /**
     * Soft delete (active=false) so historical carts/surveys keep referencing it, cascading to the
     * product's items: leaving them active would keep the SKUs purchasable and listed under a
     * product no one can reach.
     */
    public void deactivate(String id) {
        Product product = requireById(id);
        product.setActive(false);
        product.setUpdatedAt(Instant.now());
        repository.save(product);
        int items = productItems.getObject().deactivateForProduct(id);
        if (items > 0) log.info("Deactivated {} item(s) under product {}", items, id);
    }

    public Product requireById(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    private String uniqueId(String name) {
        String base = Slugify.slugify(name);
        String id = base;
        for (int i = 2; repository.existsById(id); i++) {
            id = base + "-" + i;
        }
        return id;
    }
}
