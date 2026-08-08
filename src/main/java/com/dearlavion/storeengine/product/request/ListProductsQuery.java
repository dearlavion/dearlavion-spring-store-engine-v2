package com.dearlavion.storeengine.product.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** Bound from query params via Spring's default @ModelAttribute-style GET binding — needs standard
 * JavaBean getX/setX accessors (not a record) for the data binder to populate it. */
@Getter
@Setter
public class ListProductsQuery {
    private String destination;
    private String season;
    private String party;
    private String category;
    private String search;

    @Pattern(regexp = "popular|name|default")
    private String sort;

    @Min(0)
    private Integer page;

    @Min(1)
    private Integer size;
}
