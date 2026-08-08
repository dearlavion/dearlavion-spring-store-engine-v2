package com.dearlavion.storeengine.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public storefront stats — no auth, and deliberately leaner than the admin endpoint (no
 * revenue/order-count). Currently just backs the homepage's "what's in my bag" widget. */
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 50;

    private final StatsService service;

    @GetMapping("/top-selling")
    public List<TopSellingItem> getTopSelling(@RequestParam(required = false) Integer limit) {
        int n = limit != null && limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        return service.getTopSelling(n);
    }
}
