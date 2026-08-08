package com.dearlavion.storeengine.category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Admin category management — gated by SecurityConfig's /admin/** hasRole(ADMIN) matcher. */
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categories;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category create(@Valid @RequestBody CategoryRequest body) {
        return categories.create(body.name());
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable String id, @Valid @RequestBody CategoryRequest body) {
        return categories.update(id, body.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String id) {
        categories.delete(id);
    }
}
