package com.dearlavion.storeengine.cart.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** One cart per user. */
@Getter
@Setter
@Document(collection = "carts")
public class Cart {

    @Id
    private String id;

    // name pinned to match the index Mongoose already created on the shared collection
    // (Spring Data's default @Indexed name is the bare field name, e.g. "userId", which
    // conflicts with Mongoose's "userId_1" on the same key pattern).
    @Indexed(name = "userId_1", unique = true)
    private String userId;

    private List<CartItem> items = new ArrayList<>();

    private Instant updatedAt;
}
