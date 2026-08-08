package com.dearlavion.storeengine.product;

import com.dearlavion.storeengine.common.PageResponse;
import com.dearlavion.storeengine.product.model.Product;
import com.dearlavion.storeengine.product.model.ProductFilter;
import com.dearlavion.storeengine.product.request.ListProductsQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    /** Public shop listing — filter/sort/paginate. Returns { content, page, size, total }. */
    @GetMapping("/products")
    public PageResponse<Product> list(@Valid ListProductsQuery query) {
        return service.list(ProductFilter.fromQuery(query, false));
    }

    /** Public product detail by id (a slugified name — see Product). */
    @GetMapping("/products/{id}")
    public Product getOne(@PathVariable String id) {
        return service.getById(id);
    }
}
