package com.dearlavion.storeengine.productitem.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminListProductItemsQuery {
    @NotBlank
    private String productId;
}
