package com.dearlavion.storeengine.popularkit;

import com.dearlavion.storeengine.popularkit.model.PopularKit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public: the homepage "Popular kits" section reads this. */
@RestController
@RequestMapping("/popular-kits")
@RequiredArgsConstructor
public class PopularKitController {

    private final PopularKitService service;

    @GetMapping
    public List<PopularKit> list() {
        return service.listPublic();
    }

    @GetMapping("/{idOrSlug}")
    public PopularKit get(@PathVariable String idOrSlug) {
        return service.getByIdOrSlug(idOrSlug);
    }
}
