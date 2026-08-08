package com.dearlavion.storeengine.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin-only sales statistics — best/least performing product items, derived from real order
 * history rather than a stored counter. */
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService service;

    @GetMapping("/product-items")
    public List<ProductItemStat> getProductItemStats() {
        return service.getProductItemPerformance();
    }
}
