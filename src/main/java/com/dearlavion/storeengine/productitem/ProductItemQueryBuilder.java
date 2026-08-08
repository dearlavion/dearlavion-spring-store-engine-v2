package com.dearlavion.storeengine.productitem;

import com.dearlavion.storeengine.productitem.model.ProductItemFilter;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors buildProductItemPipeline (product-item-query.ts) 1:1 — ported as literal BSON pipeline
 * stages via Aggregation.stage(Document) rather than re-derived with Spring Data's fluent
 * Aggregation DSL, to minimize behavioral drift risk on the highest-complexity piece of this port
 * (the $lookup join + computed discount fields + $facet pagination).
 */
public final class ProductItemQueryBuilder {

    private ProductItemQueryBuilder() {
    }

    public static Aggregation buildPipeline(ProductItemFilter f) {
        int page = f.page() != null ? f.page() : 0;
        int size = f.size() != null ? f.size() : 100;

        List<Document> productMatch = new ArrayList<>();
        productMatch.add(new Document("product.active", true));
        addTagMatch(productMatch, "product.destinations", f.destination());
        addTagMatch(productMatch, "product.seasons", f.season());
        addTagMatch(productMatch, "product.parties", f.party());
        if (StringUtils.hasText(f.category())) {
            productMatch.add(new Document("product.category", f.category()));
        }
        if (StringUtils.hasText(f.search())) {
            String rx = escapeRegex(f.search().trim());
            Document regex = new Document("$regex", rx).append("$options", "i");
            productMatch.add(new Document("$or", List.of(
                    new Document("product.name", regex),
                    new Document("product.description", regex),
                    new Document("product.category", regex)
            )));
        }

        Document baseMatch = new Document("active", true);
        if (StringUtils.hasText(f.productId())) baseMatch.append("productId", f.productId());
        if (StringUtils.hasText(f.id())) baseMatch.append("_id", new ObjectId(f.id()));

        List<AggregationOperation> stages = new ArrayList<>();
        stages.add(Aggregation.stage(new Document("$match", baseMatch)));
        stages.add(Aggregation.stage(new Document("$lookup", new Document("from", "products")
                .append("localField", "productId").append("foreignField", "_id").append("as", "product"))));
        stages.add(Aggregation.stage(new Document("$unwind", "$product")));
        stages.add(Aggregation.stage(new Document("$match", new Document("$and", productMatch))));

        stages.add(Aggregation.stage(new Document("$addFields", new Document("_discountAmount", new Document("$switch",
                new Document("branches", List.of(
                        new Document("case", new Document("$eq", List.of("$discountType", "percent")))
                                .append("then", new Document("$multiply", List.of("$price",
                                        new Document("$divide", List.of(new Document("$ifNull", List.of("$discountValue", 0)), 100))))),
                        new Document("case", new Document("$eq", List.of("$discountType", "amount")))
                                .append("then", new Document("$ifNull", List.of("$discountValue", 0)))
                )).append("default", 0))))));

        stages.add(Aggregation.stage(new Document("$addFields", new Document("_isDiscounted",
                new Document("$and", List.of("$onSale", new Document("$gt", List.of("$_discountAmount", 0))))))));

        stages.add(Aggregation.stage(new Document("$addFields", new Document("_effectivePrice", new Document("$cond", List.of(
                "$_isDiscounted",
                new Document("$round", List.of(new Document("$max", List.of(0, new Document("$subtract", List.of("$price", "$_discountAmount")))), 2)),
                "$price"
        ))))));

        stages.add(Aggregation.stage(new Document("$sort", determineSort(f.sort()))));

        // _id is left untouched (not renamed/excluded) — Spring Data's MappingMongoConverter maps
        // a target property literally named "id" from the document's "_id" key by convention, and
        // auto-converts ObjectId -> String for it. Explicitly projecting a same-named "id" field
        // instead (as NestJS's $toString-based pipeline does) defeats that convention and reads
        // back as null, since the converter never looks at a literal "id" key for the id property.
        Document project = new Document("productId", "$productId")
                .append("name", new Document("$ifNull", List.of("$name", "$product.name")))
                .append("brand", "$brand")
                .append("category", "$product.category")
                .append("description", "$product.description")
                .append("price", "$_effectivePrice")
                .append("originalPrice", new Document("$cond", List.of("$_isDiscounted", "$price", "$$REMOVE")))
                .append("currency", "$currency")
                .append("image", "$image")
                .append("images", new Document("$ifNull", List.of("$images", List.of())))
                .append("videos", new Document("$ifNull", List.of("$videos", List.of())))
                .append("icon", new Document("$ifNull", List.of("$icon", "$product.icon")))
                .append("stock", "$stock")
                .append("soldOut", "$soldOut")
                .append("popular", "$product.popular")
                .append("tested", "$product.tested")
                .append("destinations", "$product.destinations")
                .append("seasons", "$product.seasons")
                .append("parties", "$product.parties");
        stages.add(Aggregation.stage(new Document("$project", project)));

        Document facet = new Document("content", List.of(
                new Document("$skip", (long) page * size),
                new Document("$limit", size)
        )).append("totalCount", List.of(new Document("$count", "count")));
        stages.add(Aggregation.stage(new Document("$facet", facet)));

        return Aggregation.newAggregation(stages);
    }

    private static void addTagMatch(List<Document> and, String field, String value) {
        if (!StringUtils.hasText(value) || "All".equals(value)) return;
        and.add(new Document(field, new Document("$in", List.of(value, "All"))));
    }

    private static Document determineSort(String sort) {
        if ("popular".equals(sort)) return new Document("product.popular", -1).append("product.name", 1);
        if ("price-low".equals(sort)) return new Document("_effectivePrice", 1);
        if ("price-high".equals(sort)) return new Document("_effectivePrice", -1);
        if ("name".equals(sort)) return new Document("product.name", 1);
        return new Document("_id", 1); // natural order
    }

    /** Same hand-rolled escape as ProductQueryBuilder — MongoDB's server-side regex engine doesn't
     * understand Java's Pattern.quote() \Q..\E markers. */
    private static String escapeRegex(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (".*+?^${}()|[]\\".indexOf(c) >= 0) sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }
}
