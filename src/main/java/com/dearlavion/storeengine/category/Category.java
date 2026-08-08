package com.dearlavion.storeengine.category;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Product category (admin-managed). Backs the admin dropdown and GET /categories. */
@Getter
@Setter
@Document(collection = "categories")
public class Category {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String slug;
}
