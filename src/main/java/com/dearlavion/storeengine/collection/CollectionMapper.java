package com.dearlavion.storeengine.collection;

import com.dearlavion.storeengine.collection.model.BuiltKit;
import com.dearlavion.storeengine.collection.model.KitItem;
import com.dearlavion.storeengine.collection.request.BuiltKitRequest;
import com.dearlavion.storeengine.collection.request.KitItemRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CollectionMapper {

    public BuiltKit toBuiltKit(BuiltKitRequest dto) {
        BuiltKit kit = new BuiltKit();
        kit.setItems(toKitItems(dto.items()));
        kit.setSummary(dto.summary() != null ? dto.summary() : "");
        kit.setTitle(dto.title());
        return kit;
    }

    public List<KitItem> toKitItems(List<KitItemRequest> items) {
        List<KitItem> result = new ArrayList<>();
        for (KitItemRequest i : items) {
            KitItem item = new KitItem();
            item.setLabel(i.label());
            item.setProductId(i.productId());
            result.add(item);
        }
        return result;
    }
}
