package com.dearlavion.storeengine.productitem;

import com.dearlavion.storeengine.common.PageResponse;
import com.dearlavion.storeengine.common.exception.NotFoundException;
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
        Instant now = Instant.now();
        ProductItem item = new ProductItem();
        item.setProductId(dto.productId());
        item.setBrand(dto.brand());
        item.setSizeTier(dto.sizeTier());
        item.setSizeLabel(dto.sizeLabel());
        item.setName(dto.name());
        item.setPrice(dto.price());
        item.setCurrency(dto.currency() != null ? dto.currency() : "USD");
        item.setImage(dto.image());
        item.setImages(dto.images() != null ? dto.images() : List.of());
        item.setVideos(dto.videos() != null ? dto.videos().stream().map(ProductItemService::toVideo).toList() : List.of());
        item.setIcon(dto.icon());
        item.setStock(dto.stock() != null ? dto.stock() : 0);
        item.setSoldOut(dto.soldOut() != null && dto.soldOut());
        item.setOnSale(dto.onSale() != null && dto.onSale());
        item.setDiscountType(dto.discountType());
        item.setDiscountValue(dto.discountValue());
        item.setActive(true);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return repository.save(item);
    }

    public ProductItem update(String id, UpdateProductItemRequest dto) {
        ProductItem item = getById(id);
        if (dto.brand() != null) item.setBrand(dto.brand());
        if (dto.sizeTier() != null) item.setSizeTier(dto.sizeTier());
        if (dto.sizeLabel() != null) item.setSizeLabel(dto.sizeLabel());
        if (dto.name() != null) item.setName(dto.name());
        if (dto.price() != null) item.setPrice(dto.price());
        if (dto.currency() != null) item.setCurrency(dto.currency());
        if (dto.image() != null) item.setImage(dto.image());
        if (dto.images() != null) item.setImages(dto.images());
        if (dto.videos() != null) item.setVideos(dto.videos().stream().map(ProductItemService::toVideo).toList());
        if (dto.icon() != null) item.setIcon(dto.icon());
        if (dto.stock() != null) item.setStock(dto.stock());
        if (dto.soldOut() != null) item.setSoldOut(dto.soldOut());
        if (dto.active() != null) item.setActive(dto.active());
        if (dto.onSale() != null) item.setOnSale(dto.onSale());
        if (dto.discountType() != null) item.setDiscountType(dto.discountType());
        if (dto.discountValue() != null) item.setDiscountValue(dto.discountValue());
        item.setUpdatedAt(Instant.now());
        return repository.save(item);
    }

    /** Soft delete (active=false) — matches Product's own delete convention. */
    public void deactivate(String id) {
        ProductItem item = getById(id);
        item.setActive(false);
        item.setUpdatedAt(Instant.now());
        repository.save(item);
    }

    private static ProductVideo toVideo(ProductVideoRequest req) {
        ProductVideo video = new ProductVideo();
        video.setTitle(req.title());
        video.setUrl(req.url());
        video.setAuthor(req.author());
        return video;
    }
}
