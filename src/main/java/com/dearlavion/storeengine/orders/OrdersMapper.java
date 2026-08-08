package com.dearlavion.storeengine.orders;

import com.dearlavion.storeengine.orders.model.OrderItem;
import com.dearlavion.storeengine.orders.model.Shipping;
import com.dearlavion.storeengine.orders.request.PlaceOrderRequest;
import com.dearlavion.storeengine.orders.request.ShippingRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrdersMapper {

    public List<OrderItem> toOrderItems(PlaceOrderRequest dto) {
        return dto.items().stream().map(i -> {
            OrderItem item = new OrderItem();
            item.setProductId(i.productId());
            item.setProductItemId(i.productItemId());
            item.setBrand(i.brand());
            item.setName(i.name());
            item.setIcon(i.icon() != null ? i.icon() : "");
            item.setQuantity(i.quantity());
            item.setPrice(i.price());
            item.setCurrency(i.currency() != null ? i.currency() : (dto.currency() != null ? dto.currency() : "USD"));
            item.setInventoryUpdated(false);
            return item;
        }).toList();
    }

    public Shipping toShipping(ShippingRequest dto) {
        if (dto == null) return null;
        Shipping shipping = new Shipping();
        shipping.setFullName(dto.fullName());
        shipping.setEmail(dto.email());
        shipping.setAddress(dto.address());
        shipping.setCity(dto.city());
        shipping.setPostalCode(dto.postalCode());
        return shipping;
    }
}
