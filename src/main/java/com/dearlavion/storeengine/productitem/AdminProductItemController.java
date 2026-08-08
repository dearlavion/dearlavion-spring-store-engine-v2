package com.dearlavion.storeengine.productitem;

import com.dearlavion.storeengine.product.ProductService;
import com.dearlavion.storeengine.productitem.model.ProductItem;
import com.dearlavion.storeengine.productitem.request.AdminListProductItemsQuery;
import com.dearlavion.storeengine.productitem.request.CreateProductItemRequest;
import com.dearlavion.storeengine.productitem.request.UpdateProductItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin ProductItem management — fully separate from /admin/products (productId is a query param
 * on list, a body field on create, rather than a nested URL segment). */
@RestController
@RequestMapping("/admin/product-items")
@RequiredArgsConstructor
public class AdminProductItemController {

    private final ProductItemService service;
    private final ProductService productService;

    /** Admin listing includes inactive items. */
    @GetMapping
    public List<ProductItem> list(@Valid AdminListProductItemsQuery query) {
        return service.listAll(query.getProductId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductItem create(@Valid @RequestBody CreateProductItemRequest dto) {
        // Confirms the parent exists before attaching a child item to it.
        productService.requireById(dto.productId());
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ProductItem update(@PathVariable String id, @Valid @RequestBody UpdateProductItemRequest dto) {
        return service.update(id, dto);
    }

    @PatchMapping("/{id}")
    public ProductItem patch(@PathVariable String id, @Valid @RequestBody UpdateProductItemRequest dto) {
        return service.update(id, dto);
    }

    /** Soft delete (active=false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String id) {
        service.deactivate(id);
    }
}
