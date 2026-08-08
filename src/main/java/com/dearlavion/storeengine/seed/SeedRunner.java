package com.dearlavion.storeengine.seed;

import com.dearlavion.storeengine.category.CategoryService;
import com.dearlavion.storeengine.product.ProductRepository;
import com.dearlavion.storeengine.product.model.Product;
import com.dearlavion.storeengine.productitem.model.ProductItem;
import com.dearlavion.storeengine.productitem.ProductItemRepository;
import com.dearlavion.storeengine.taxonomy.TaxonomyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Direct port of dearlavion-store-engine's src/seed/seed.ts: idempotently upserts the 8
 * categories, 8 taxonomy axes ({@link TaxonomySeedData}), and the 50-product mock catalog (plus a
 * default ProductItem per product) from the same seed/categories.json and seed/seed-data.json
 * bundled as classpath resources. Only runs under the "seed" Spring profile — never on a normal
 * boot — so this never touches the shared production database by accident:
 * {@code mvn spring-boot:run -Dspring-boot.run.profiles=seed}.
 */
@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class SeedRunner implements CommandLineRunner {

    private final CategoryService categoryService;
    private final TaxonomyService taxonomyService;
    private final ProductRepository productRepository;
    private final ProductItemRepository productItemRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        List<String> categories = objectMapper.readValue(
                new ClassPathResource("seed/categories.json").getInputStream(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                });
        for (String name : categories) {
            categoryService.upsert(name);
        }

        for (TaxonomySeedData.Entry entry : TaxonomySeedData.ENTRIES) {
            taxonomyService.upsert(entry.axis(), entry.value(), entry.order(), entry.emoji(), entry.subtext(), entry.code());
        }

        List<SeedProductEntry> products = objectMapper.readValue(
                new ClassPathResource("seed/seed-data.json").getInputStream(), new com.fasterxml.jackson.core.type.TypeReference<List<SeedProductEntry>>() {
                });
        int productUpserts = 0;
        int itemInserts = 0;
        for (SeedProductEntry p : products) {
            String id = com.dearlavion.storeengine.common.Slugify.slugify(p.name());
            Instant now = Instant.now();

            Product product = productRepository.findById(id).orElseGet(() -> {
                Product created = new Product();
                created.setId(id);
                created.setCreatedAt(now);
                created.setLinkedProductIds(List.of());
                return created;
            });
            product.setName(p.name());
            product.setCategory(p.category());
            product.setDescription(p.description());
            product.setIcon(p.icon());
            product.setPopular(Boolean.TRUE.equals(p.popular()));
            product.setTested(Boolean.TRUE.equals(p.tested()));
            if (p.destinations() != null) product.setDestinations(p.destinations());
            if (p.seasons() != null) product.setSeasons(p.seasons());
            if (p.parties() != null) product.setParties(p.parties());
            product.setActive(true);
            product.setUpdatedAt(now);
            productRepository.save(product);
            productUpserts++;

            // Only seed a default ProductItem the first time this product is created — never
            // overwrite admin-made price/stock/name edits on a re-run.
            if (productItemRepository.findByProductId(id, org.springframework.data.domain.Sort.unsorted()).isEmpty()) {
                int stock = deterministicStock(id);
                ProductItem item = new ProductItem();
                item.setProductId(id);
                item.setName(p.name());
                item.setPrice(p.price() != null ? p.price() : 0);
                item.setCurrency(p.currency() != null ? p.currency() : "USD");
                item.setIcon(p.icon());
                item.setStock(stock);
                item.setSoldOut(stock == 0);
                item.setActive(true);
                item.setCreatedAt(now);
                item.setUpdatedAt(now);
                productItemRepository.save(item);
                itemInserts++;
            }
        }

        log.info("Seeded {} categories, {} taxonomy values, {} products ({} new default product items).",
                categories.size(), TaxonomySeedData.ENTRIES.size(), productUpserts, itemInserts);
    }

    /** Stable, varied stock (0-59) derived from the slug — same hash as seed.ts's
     * deterministicStock() — so a given product always seeds the same count. */
    private static int deterministicStock(String slug) {
        long h = 0;
        for (int i = 0; i < slug.length(); i++) {
            h = (h * 31 + slug.charAt(i)) & 0xFFFFFFFFL;
        }
        return (int) (h % 60);
    }
}
