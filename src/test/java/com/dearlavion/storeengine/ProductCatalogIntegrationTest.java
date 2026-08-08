package com.dearlavion.storeengine;

import com.dearlavion.storeengine.security.AuthClientService;
import com.dearlavion.storeengine.security.VerifyResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * End-to-end verification of Phase 1 (product, product-item, taxonomy, category) against a real
 * MongoDB (Testcontainers), with AuthClientService mocked exactly the way the NestJS source's own
 * store.integration.spec.ts overrides AuthClientService — any bearer token verifies as a fixed
 * admin identity, so this proves the actual CRUD/aggregation/security-matcher behavior without
 * depending on a real, shared auth-service-v2 instance.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductCatalogIntegrationTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
    }

    @Autowired
    private TestRestTemplate rest;

    @MockBean
    private AuthClientService authClient;

    private static final HttpHeaders ADMIN_HEADERS = new HttpHeaders();

    @BeforeAll
    static void setHeaders() {
        ADMIN_HEADERS.set("Authorization", "Bearer test-token");
    }

    /** The default JDK HttpURLConnection-backed request factory can't retry a POST past a 401
     * response ("cannot retry due to server authentication, in streaming mode") — swap in Apache
     * HttpClient5, which doesn't have this quirk, so the 401 test can actually read the response. */
    @BeforeEach
    void useHttpComponentsRequestFactory() {
        rest.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    private void stubAdminAuth() {
        VerifyResponse response = new VerifyResponse();
        response.setValid(true);
        response.setUserId("test-admin-id");
        response.setUsername("test-admin");
        response.setEmail("admin@test.dev");
        response.setActiveProfile("ADMIN");
        when(authClient.verify(anyString())).thenReturn(response);
        when(authClient.expectedCustomer()).thenReturn("");
    }

    @Test
    void adminWriteWithoutTokenIs403() {
        ResponseEntity<String> res = rest.postForEntity("/admin/categories", Map.of("name", "Test"), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminWriteWithInvalidTokenIs401() {
        when(authClient.verify(anyString())).thenReturn(null);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer garbage");
        ResponseEntity<Map> res = rest.exchange("/admin/categories", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Test"), headers), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).containsEntry("statusCode", 401);
    }

    @Test
    void categoryCrudRoundTrip() {
        stubAdminAuth();

        ResponseEntity<Map> created = rest.exchange("/admin/categories", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Electronics"), ADMIN_HEADERS), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).containsEntry("name", "Electronics").containsEntry("slug", "electronics");

        ResponseEntity<List> list = rest.getForEntity("/categories", List.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).hasSize(1);
    }

    @Test
    void productAndProductItemAggregationPipeline() {
        stubAdminAuth();

        // Create a product tagged for Beach/Summer.
        Map<String, Object> productBody = Map.of(
                "name", "Test Sunscreen",
                "category", "Toiletries",
                "destinations", List.of("Beach"),
                "seasons", List.of("Summer"),
                "kitCategory", "Toiletries"
        );
        ResponseEntity<Map> productRes = rest.exchange("/admin/products", HttpMethod.POST,
                new HttpEntity<>(productBody, ADMIN_HEADERS), Map.class);
        assertThat(productRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String productId = (String) productRes.getBody().get("id");
        assertThat(productId).isEqualTo("test-sunscreen");

        // Public product read.
        ResponseEntity<Map> getOne = rest.getForEntity("/products/" + productId, Map.class);
        assertThat(getOne.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getOne.getBody()).containsEntry("name", "Test Sunscreen");

        // Create a ProductItem (SKU) with a 20% discount, onSale=true.
        Map<String, Object> itemBody = Map.of(
                "productId", productId,
                "name", "Test Sunscreen 50ml",
                "price", 20.0,
                "onSale", true,
                "discountType", "percent",
                "discountValue", 20,
                "stock", 10
        );
        ResponseEntity<Map> itemRes = rest.exchange("/admin/product-items", HttpMethod.POST,
                new HttpEntity<>(itemBody, ADMIN_HEADERS), Map.class);
        assertThat(itemRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Public listing — proves the $lookup/$facet/discount-computation aggregation pipeline
        // actually runs correctly end-to-end against real Mongo, not just compiles.
        ResponseEntity<Map> listRes = rest.getForEntity("/product-items?productId=" + productId, Map.class);
        assertThat(listRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> content = (List<Map<String, Object>>) listRes.getBody().get("content");
        assertThat(content).hasSize(1);
        Map<String, Object> item = content.get(0);
        // Regression guard: Spring Data's id-property convention binds a property named "id" from
        // the document's "_id" key, not a same-named projected "id" field — a naive $toString-based
        // rename (mirroring the NestJS pipeline literally) reads back as null. See
        // ProductItemQueryBuilder's $project comment for the fix.
        assertThat(item.get("id")).isNotNull();
        assertThat(item.get("price")).isEqualTo(16.0); // 20 - 20% = 16, computed by the pipeline
        assertThat(item.get("originalPrice")).isEqualTo(20.0);
        assertThat(item.get("category")).isEqualTo("Toiletries"); // joined from the parent product
        assertThat(item.get("destinations")).isEqualTo(List.of("Beach"));

        // Beach/Summer filter should match; Mountain/Winter should not.
        ResponseEntity<Map> matchRes = rest.getForEntity("/product-items?destination=Beach&season=Summer", Map.class);
        List<?> matched = (List<?>) matchRes.getBody().get("content");
        assertThat(matched).hasSize(1);

        ResponseEntity<Map> noMatchRes = rest.getForEntity("/product-items?destination=Mountain", Map.class);
        List<?> noMatch = (List<?>) noMatchRes.getBody().get("content");
        assertThat(noMatch).isEmpty();
    }

    @Test
    void taxonomyAndAxisOrder() {
        stubAdminAuth();

        ResponseEntity<Map> created = rest.exchange("/admin/taxonomies", HttpMethod.POST,
                new HttpEntity<>(Map.of("axis", "destination", "value", "Desert", "order", 4), ADMIN_HEADERS), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<List> list = rest.getForEntity("/taxonomies", List.class);
        assertThat(list.getBody()).hasSize(1);

        // Duration is fixed-cardinality — adding a value to it must be rejected.
        ResponseEntity<Map> durationAdd = rest.exchange("/admin/taxonomies", HttpMethod.POST,
                new HttpEntity<>(Map.of("axis", "duration", "value", "Extra Long"), ADMIN_HEADERS), Map.class);
        assertThat(durationAdd.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<Map> axisOrderRes = rest.exchange("/admin/taxonomies/axis-order", HttpMethod.PUT,
                new HttpEntity<>(Map.of("order", List.of("season", "destination")), ADMIN_HEADERS), Map.class);
        assertThat(axisOrderRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(axisOrderRes.getBody()).containsEntry("order", List.of("season", "destination"));

        ResponseEntity<Map> getOrder = rest.getForEntity("/taxonomies/axis-order", Map.class);
        assertThat(getOrder.getBody()).containsEntry("order", List.of("season", "destination"));
    }
}
