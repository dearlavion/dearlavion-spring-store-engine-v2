package com.dearlavion.storeengine.productitem;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListProductItemsQuery {
    private String id; // a single item's own id — narrows to exactly one result
    private String productId;
    private String destination;
    private String season;
    private String party;
    private String category;
    private String search;

    @Pattern(regexp = "popular|price-low|price-high|name|default")
    private String sort;

    @Min(0)
    private Integer page;

    @Min(1)
    private Integer size;
}
