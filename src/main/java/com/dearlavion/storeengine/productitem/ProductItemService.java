package com.dearlavion.storeengine.productitem;

import com.dearlavion.storeengine.common.PageResponse;
import com.dearlavion.storeengine.common.exception.NotFoundException;
import com.dearlavion.storeengine.productitem.model.ProductItem;
import com.dearlavion.storeengine.productitem.model.ProductItemFacetResult;
import com.dearlavion.storeengine.productitem.model.ProductItemFilter;
import com.dearlavion.storeengine.productitem.request.CreateProductItemRequest;
import com.dearlavion.storeengine.productitem.request.UpdateProductItemRequest;
import com.dearlavion.storeengine.productitem.response.ProductItemView;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductItemService {

    private final ProductItemRepository repository;
    private final MongoTemplate mongoTemplate;
    private final ProductItemMapper mapper;

    /** The main storefront listing (Shop) — every active, purchasable item across the whole
     * catalog, joined with its parent Product for tag filtering/display. filter.id() narrows to a
     * single item (still returned inside `content`, as a 0-or-1-length page) rather than being a
     * separate endpoint. */
    public PageResponse<ProductItemView> listCatalog(ProductItemFilter filter) {
        int page = filter.page() != null ? filter.page() : 0;
        int size = filter.size() != null ? filter.size() : 100;
        if (filter.id() != null && !ObjectId.isValid(filter.id())) {
            return new PageResponse<>(List.of(), page, size, 0);
        }
        Aggregation pipeline = ProductItemQueryBuilder.buildPipeline(filter);
        AggregationResults<ProductItemFacetResult> results =
                mongoTemplate.aggregate(pipeline, "product_items", ProductItemFacetResult.class);
        ProductItemFacetResult result = results.getUniqueMappedResult();
        List<ProductItemView> content = result != null ? result.content() : List.of();
        long total = result != null ? result.total() : 0;
        return new PageResponse<>(content, page, size, total);
    }

    /** Admin: every item for a product, including inactive. */
    public List<ProductItem> listAll(String productId) {
        return repository.findByProductId(productId, Sort.by(Sort.Order.asc("price")));
    }

    /** The cheapest active item for a product — used to resolve "suggest/display this product"
     * down to something actually sellable. */
    public ProductItem getDefault(String productId) {
        List<ProductItem> items = repository.findByProductIdAndActiveTrue(productId, Sort.by(Sort.Order.asc("price")));
        return items.isEmpty() ? null : items.get(0);
    }

    /** All active items for a set of products, cheapest first — for size-aware kit resolution. */
    public List<ProductItem> activeForProducts(List<String> productIds) {
        return repository.findByProductIdInAndActiveTrue(productIds, Sort.by(Sort.Order.asc("price")));
    }

    public ProductItem getById(String id) {
        if (!ObjectId.isValid(id)) throw new NotFoundException("Product item not found: " + id);
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Product item not found: " + id));
    }

    public ProductItem create(CreateProductItemRequest dto) {
        return repository.save(mapper.toEntity(dto));
    }

    public ProductItem update(String id, UpdateProductItemRequest dto) {
        ProductItem item = getById(id);
        mapper.applyPatch(item, dto);
        return repository.save(item);
    }

    /** Soft delete (active=false) — matches Product's own delete convention. */
    public void deactivate(String id) {
        ProductItem item = getById(id);
        item.setActive(false);
        item.setUpdatedAt(Instant.now());
        repository.save(item);
    }

    /**
     * Deactivates every item under a product — called when the product itself is deleted, so its
     * SKUs don't stay purchasable behind a product nobody can reach. Soft, like every other delete
     * here: historical carts and orders keep resolving their items.
     *
     * @return how many were deactivated
     */
    public int deactivateForProduct(String productId) {
        List<ProductItem> items = repository.findByProductIdAndActiveTrue(productId, Sort.unsorted());
        Instant now = Instant.now();
        items.forEach(i -> { i.setActive(false); i.setUpdatedAt(now); });
        repository.saveAll(items);
        return items.size();
    }
}
