package com.dearlavion.storeengine.product;

import com.dearlavion.storeengine.common.PageResponse;
import com.dearlavion.storeengine.common.Slugify;
import com.dearlavion.storeengine.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final MongoTemplate mongoTemplate;

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
        Instant now = Instant.now();
        Product product = new Product();
        product.setId(uniqueId(dto.name()));
        product.setName(dto.name());
        product.setCategory(dto.category());
        product.setDescription(dto.description());
        product.setIcon(dto.icon());
        product.setPopular(dto.popular() != null && dto.popular());
        product.setTested(dto.tested() != null && dto.tested());
        product.setDestinations(dto.destinations() != null ? dto.destinations() : List.of("All"));
        product.setSeasons(dto.seasons() != null ? dto.seasons() : List.of("All"));
        product.setParties(dto.parties() != null ? dto.parties() : List.of("All"));
        product.setActivities(dto.activities() != null ? dto.activities() : List.of());
        product.setTransportModes(dto.transportModes() != null ? dto.transportModes() : List.of());
        product.setKitCategory(dto.kitCategory());
        product.setActive(true);
        product.setLinkedProductIds(dto.linkedProductIds() != null ? dto.linkedProductIds() : List.of());
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        return repository.save(product);
    }

    /** id is fixed at creation and never changes here, even when dto.name() renames the product —
     * avoids invalidating every existing reference to this product (ProductItem.productId,
     * PopularKit/Product.linkedProductIds, saved carts/kits) on a simple rename. */
    public Product update(String id, UpdateProductRequest dto) {
        Product product = requireById(id);
        if (dto.name() != null) product.setName(dto.name());
        if (dto.category() != null) product.setCategory(dto.category());
        if (dto.description() != null) product.setDescription(dto.description());
        if (dto.icon() != null) product.setIcon(dto.icon());
        if (dto.popular() != null) product.setPopular(dto.popular());
        if (dto.tested() != null) product.setTested(dto.tested());
        if (dto.destinations() != null) product.setDestinations(dto.destinations());
        if (dto.seasons() != null) product.setSeasons(dto.seasons());
        if (dto.parties() != null) product.setParties(dto.parties());
        if (dto.activities() != null) product.setActivities(dto.activities());
        if (dto.transportModes() != null) product.setTransportModes(dto.transportModes());
        if (dto.kitCategory() != null) product.setKitCategory(dto.kitCategory());
        if (dto.active() != null) product.setActive(dto.active());
        if (dto.linkedProductIds() != null) product.setLinkedProductIds(dto.linkedProductIds());
        product.setUpdatedAt(Instant.now());
        return repository.save(product);
    }

    /** Soft delete (active=false) so historical carts/surveys keep referencing it. */
    public void deactivate(String id) {
        Product product = requireById(id);
        product.setActive(false);
        product.setUpdatedAt(Instant.now());
        repository.save(product);
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
