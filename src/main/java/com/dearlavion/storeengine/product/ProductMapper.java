package com.dearlavion.storeengine.product;

import com.dearlavion.storeengine.product.model.Product;
import com.dearlavion.storeengine.product.request.CreateProductRequest;
import com.dearlavion.storeengine.product.request.UpdateProductRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ProductMapper {

    public Product toEntity(CreateProductRequest dto, String id) {
        Instant now = Instant.now();
        Product product = new Product();
        product.setId(id);
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
        return product;
    }

    /** id is fixed at creation and never changes here, even when dto.name() renames the product —
     * avoids invalidating every existing reference to this product (ProductItem.productId,
     * PopularKit/Product.linkedProductIds, saved carts/kits) on a simple rename. */
    public void applyPatch(Product product, UpdateProductRequest dto) {
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
    }
}
