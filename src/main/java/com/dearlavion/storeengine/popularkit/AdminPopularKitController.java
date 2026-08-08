package com.dearlavion.storeengine.popularkit;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin curation of the storefront's Popular Kits section. */
@RestController
@RequestMapping("/admin/popular-kits")
@RequiredArgsConstructor
public class AdminPopularKitController {

    private final PopularKitService service;

    /** Admin listing includes deactivated kits. */
    @GetMapping
    public List<PopularKit> list() {
        return service.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PopularKit create(@Valid @RequestBody CreatePopularKitRequest dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public PopularKit update(@PathVariable String id, @Valid @RequestBody UpdatePopularKitRequest dto) {
        return service.update(id, dto);
    }

    @PatchMapping("/{id}")
    public PopularKit patch(@PathVariable String id, @Valid @RequestBody UpdatePopularKitRequest dto) {
        return service.update(id, dto);
    }

    /** Soft delete (active=false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String id) {
        service.deactivate(id);
    }
}
