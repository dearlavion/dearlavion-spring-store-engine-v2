package com.dearlavion.storeengine.product;

import com.dearlavion.storeengine.common.PageResponse;
import com.dearlavion.storeengine.product.model.Product;
import com.dearlavion.storeengine.product.model.ProductFilter;
import com.dearlavion.storeengine.product.request.CreateProductRequest;
import com.dearlavion.storeengine.product.request.ListProductsQuery;
import com.dearlavion.storeengine.product.request.UpdateProductRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Admin product management — gated by SecurityConfig's /admin/** hasRole(ADMIN) matcher. */
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService service;

    /** Admin listing includes inactive products. */
    @GetMapping
    public PageResponse<Product> list(@Valid ListProductsQuery query) {
        return service.list(ProductFilter.fromQuery(query, true));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody CreateProductRequest dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable String id, @RequestBody UpdateProductRequest dto) {
        return service.update(id, dto);
    }

    @PatchMapping("/{id}")
    public Product patch(@PathVariable String id, @RequestBody UpdateProductRequest dto) {
        return service.update(id, dto);
    }

    /** Soft delete (active=false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String id) {
        service.deactivate(id);
    }
}
