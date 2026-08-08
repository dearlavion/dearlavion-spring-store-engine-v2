package com.dearlavion.storeengine.productitem;

import com.dearlavion.storeengine.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public ProductItem reads — fully separate from /products (generics never independently list
 * as purchasable; this is what Shop/Product Detail actually render). ?productId=X filters down to
 * one product's items; ?id=X loads exactly one item by its own id. */
@RestController
@RequiredArgsConstructor
public class ProductItemController {

    private final ProductItemService service;

    @GetMapping("/product-items")
    public PageResponse<ProductItemView> list(@Valid ListProductItemsQuery query) {
        return service.listCatalog(ProductItemFilter.fromQuery(query));
    }
}
