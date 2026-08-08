package com.dearlavion.storeengine.productitem;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminListProductItemsQuery {
    @NotBlank
    private String productId;
}
