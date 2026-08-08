package com.dearlavion.storeengine.productitem;

import com.dearlavion.storeengine.productitem.model.ProductItem;
import com.dearlavion.storeengine.productitem.model.ProductVideo;
import com.dearlavion.storeengine.productitem.request.CreateProductItemRequest;
import com.dearlavion.storeengine.productitem.request.ProductVideoRequest;
import com.dearlavion.storeengine.productitem.request.UpdateProductItemRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ProductItemMapper {

    public ProductItem toEntity(CreateProductItemRequest dto) {
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
        item.setVideos(dto.videos() != null ? dto.videos().stream().map(this::toVideo).toList() : List.of());
        item.setIcon(dto.icon());
        item.setStock(dto.stock() != null ? dto.stock() : 0);
        item.setSoldOut(dto.soldOut() != null && dto.soldOut());
        item.setOnSale(dto.onSale() != null && dto.onSale());
        item.setDiscountType(dto.discountType());
        item.setDiscountValue(dto.discountValue());
        item.setActive(true);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return item;
    }

    public void applyPatch(ProductItem item, UpdateProductItemRequest dto) {
        if (dto.brand() != null) item.setBrand(dto.brand());
        if (dto.sizeTier() != null) item.setSizeTier(dto.sizeTier());
        if (dto.sizeLabel() != null) item.setSizeLabel(dto.sizeLabel());
        if (dto.name() != null) item.setName(dto.name());
        if (dto.price() != null) item.setPrice(dto.price());
        if (dto.currency() != null) item.setCurrency(dto.currency());
        if (dto.image() != null) item.setImage(dto.image());
        if (dto.images() != null) item.setImages(dto.images());
        if (dto.videos() != null) item.setVideos(dto.videos().stream().map(this::toVideo).toList());
        if (dto.icon() != null) item.setIcon(dto.icon());
        if (dto.stock() != null) item.setStock(dto.stock());
        if (dto.soldOut() != null) item.setSoldOut(dto.soldOut());
        if (dto.active() != null) item.setActive(dto.active());
        if (dto.onSale() != null) item.setOnSale(dto.onSale());
        if (dto.discountType() != null) item.setDiscountType(dto.discountType());
        if (dto.discountValue() != null) item.setDiscountValue(dto.discountValue());
        item.setUpdatedAt(Instant.now());
    }

    private ProductVideo toVideo(ProductVideoRequest req) {
        ProductVideo video = new ProductVideo();
        video.setTitle(req.title());
        video.setUrl(req.url());
        video.setAuthor(req.author());
        return video;
    }
}
