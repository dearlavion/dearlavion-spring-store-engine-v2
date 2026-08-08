package com.dearlavion.storeengine.category;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public: list categories (backs the shop filter + admin dropdown). */
@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categories;

    @GetMapping("/categories")
    public List<Category> list() {
        return categories.list();
    }
}
