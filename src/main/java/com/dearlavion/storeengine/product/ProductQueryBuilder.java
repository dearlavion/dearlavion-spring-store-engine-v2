package com.dearlavion.storeengine.product;

import com.dearlavion.storeengine.product.model.ProductFilter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** Pure builder for the product-listing Mongo query — ported 1:1 from product-query.ts. A tag
 * filter matches products tagged with the requested value OR 'All' (mirrors the frontend's shop
 * filtering). Only active products unless includeInactive is set. */
public final class ProductQueryBuilder {

    private ProductQueryBuilder() {
    }

    public static Query buildQuery(ProductFilter f) {
        List<Criteria> and = new ArrayList<>();

        if (!f.includeInactive()) and.add(Criteria.where("active").is(true));

        tag(and, "destinations", f.destination());
        tag(and, "seasons", f.season());
        tag(and, "parties", f.party());

        if (StringUtils.hasText(f.category())) {
            and.add(Criteria.where("category").is(f.category()));
        }

        if (StringUtils.hasText(f.search())) {
            String rx = escapeRegex(f.search().trim());
            and.add(new Criteria().orOperator(
                    Criteria.where("name").regex(rx, "i"),
                    Criteria.where("description").regex(rx, "i"),
                    Criteria.where("category").regex(rx, "i")
            ));
        }

        Query query = and.isEmpty() ? new Query() : new Query(new Criteria().andOperator(and.toArray(new Criteria[0])));
        Sort sort = determineSort(f.sort());
        if (sort != null) query.with(sort);
        return query;
    }

    private static void tag(List<Criteria> and, String field, String value) {
        if (!StringUtils.hasText(value) || "All".equals(value)) return;
        and.add(Criteria.where(field).in(value, "All"));
    }

    private static Sort determineSort(String sort) {
        if ("popular".equals(sort)) return Sort.by(Sort.Order.desc("popular"), Sort.Order.asc("name"));
        if ("name".equals(sort)) return Sort.by(Sort.Order.asc("name"));
        return null; // natural order
    }

    /** Mirrors product-query.ts's escapeRegex — MongoDB's server-side regex engine doesn't
     * understand Java's Pattern.quote() \Q..\E markers, so this escapes the same character class
     * by hand rather than relying on java.util.regex.Pattern helpers. */
    static String escapeRegex(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (".*+?^${}()|[]\\".indexOf(c) >= 0) sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }
}
