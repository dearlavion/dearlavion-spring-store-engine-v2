package com.dearlavion.storeengine.category;

import com.dearlavion.storeengine.common.Slugify;
import com.dearlavion.storeengine.common.exception.ConflictException;
import com.dearlavion.storeengine.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;

    public List<Category> list() {
        return repository.findAllByOrderByNameAsc();
    }

    public Category create(String name) {
        String slug = Slugify.slugify(name);
        if (repository.findBySlug(slug).isPresent()) {
            throw new ConflictException("Category already exists");
        }
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        return repository.save(category);
    }

    public Category update(String id, String name) {
        Category category = repository.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
        category.setName(name);
        category.setSlug(Slugify.slugify(name));
        return repository.save(category);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    /** Idempotent upsert by slug — used by the seed script. */
    public void upsert(String name) {
        String slug = Slugify.slugify(name);
        Category category = repository.findBySlug(slug).orElseGet(Category::new);
        category.setName(name);
        category.setSlug(slug);
        repository.save(category);
    }
}
