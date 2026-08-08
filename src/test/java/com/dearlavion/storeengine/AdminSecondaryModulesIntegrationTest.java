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
 * End-to-end verification of Phase 3 (popular-kit, store-settings, exchange-rate, profile, stats,
 * newsletter, collection) against a real MongoDB (Testcontainers), with AuthClientService mocked
 * so every bearer token verifies as a fixed identity.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminSecondaryModulesIntegrationTest {

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

    private static final HttpHeaders AUTH_HEADERS = new HttpHeaders();

    @BeforeAll
    static void setHeaders() {
        AUTH_HEADERS.set("Authorization", "Bearer test-token");
    }

    @BeforeEach
    void setup() {
        rest.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        VerifyResponse response = new VerifyResponse();
        response.setValid(true);
        response.setUserId("test-user-id");
        response.setUsername("test-user");
        response.setEmail("user@test.dev");
        response.setActiveProfile("ADMIN");
        when(authClient.verify(anyString())).thenReturn(response);
        when(authClient.expectedCustomer()).thenReturn("");
    }

    @Test
    void popularKitCrudAndSoftDelete() {
        Map<String, Object> body = Map.of("name", "Beach Essentials Kit", "productIds", List.of("a", "b"));
        ResponseEntity<Map> created = rest.exchange("/admin/popular-kits", HttpMethod.POST,
                new HttpEntity<>(body, AUTH_HEADERS), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String slug = (String) created.getBody().get("slug");
        assertThat(slug).isEqualTo("beach-essentials-kit");

        ResponseEntity<List> publicList = rest.getForEntity("/popular-kits", List.class);
        assertThat(publicList.getBody()).hasSize(1);

        ResponseEntity<Map> bySlug = rest.getForEntity("/popular-kits/" + slug, Map.class);
        assertThat(bySlug.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bySlug.getBody()).containsEntry("name", "Beach Essentials Kit");

        String id = (String) created.getBody().get("id");
        ResponseEntity<Map> updated = rest.exchange("/admin/popular-kits/" + id, HttpMethod.PUT,
                new HttpEntity<>(Map.of("tag", "Bestseller"), AUTH_HEADERS), Map.class);
        assertThat(updated.getBody()).containsEntry("tag", "Bestseller").containsEntry("slug", slug);

        rest.exchange("/admin/popular-kits/" + id, HttpMethod.DELETE, new HttpEntity<>(AUTH_HEADERS), Void.class);
        ResponseEntity<List> afterDeactivate = rest.getForEntity("/popular-kits", List.class);
        assertThat(afterDeactivate.getBody()).isEmpty();

        ResponseEntity<List> adminListIncludesInactive = rest.exchange("/admin/popular-kits", HttpMethod.GET,
                new HttpEntity<>(AUTH_HEADERS), List.class);
        assertThat(adminListIncludesInactive.getBody()).hasSize(1);
    }

    @Test
    void storeSettingsAndExchangeRatesRoundTrip() {
        ResponseEntity<Map> settingsUpdate = rest.exchange("/admin/store-settings", HttpMethod.PUT,
                new HttpEntity<>(Map.of("freeShippingMinimum", 75, "shippingFee", 5.5), AUTH_HEADERS), Map.class);
        assertThat(settingsUpdate.getBody()).containsEntry("freeShippingMinimum", 75.0).containsEntry("shippingFee", 5.5);

        ResponseEntity<Map> publicSettings = rest.getForEntity("/store-settings", Map.class);
        assertThat(publicSettings.getBody()).containsEntry("freeShippingMinimum", 75.0);

        ResponseEntity<Map> ratesUpdate = rest.exchange("/admin/exchange-rates", HttpMethod.PUT,
                new HttpEntity<>(Map.of("rates", Map.of("PHP", 60.0)), AUTH_HEADERS), Map.class);
        Map<String, Object> rates = (Map<String, Object>) ratesUpdate.getBody().get("rates");
        assertThat(rates).containsEntry("USD", 1.0).containsEntry("PHP", 60.0);
        assertThat(rates).containsKey("JPY"); // falls back to the default since it was never set

        ResponseEntity<Map> publicRates = rest.getForEntity("/exchange-rates", Map.class);
        Map<String, Object> publicRatesMap = (Map<String, Object>) publicRates.getBody().get("rates");
        assertThat(publicRatesMap).containsEntry("PHP", 60.0);
    }

    @Test
    void profileGetCreatesDefaultAndUpdateValidatesCurrency() {
        ResponseEntity<Map> initial = rest.exchange("/profile", HttpMethod.GET, new HttpEntity<>(AUTH_HEADERS), Map.class);
        assertThat(initial.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(initial.getBody()).containsEntry("displayName", "Traveler").containsEntry("currency", "USD");

        ResponseEntity<Map> updated = rest.exchange("/profile", HttpMethod.PUT,
                new HttpEntity<>(Map.of("displayName", "Alex", "currency", "PHP"), AUTH_HEADERS), Map.class);
        assertThat(updated.getBody()).containsEntry("displayName", "Alex").containsEntry("currency", "PHP");

        ResponseEntity<Map> invalidCurrency = rest.exchange("/profile", HttpMethod.PUT,
                new HttpEntity<>(Map.of("currency", "XXX"), AUTH_HEADERS), Map.class);
        assertThat(invalidCurrency.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void newsletterSubscribeIsIdempotent() {
        ResponseEntity<Map> first = rest.postForEntity("/newsletter/subscribe", Map.of("email", "Traveler@Example.com"), Map.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody()).containsEntry("subscribed", true);

        ResponseEntity<Map> second = rest.postForEntity("/newsletter/subscribe", Map.of("email", "traveler@example.com"), Map.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void statsEndpointsRespond() {
        ResponseEntity<List> publicTopSelling = rest.getForEntity("/stats/top-selling?limit=5", List.class);
        assertThat(publicTopSelling.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<List> adminStats = rest.exchange("/admin/stats/product-items", HttpMethod.GET,
                new HttpEntity<>(AUTH_HEADERS), List.class);
        assertThat(adminStats.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void savedKitCrudRoundTrip() {
        Map<String, Object> saveBody = Map.of(
                "name", "My Beach Trip",
                "kit", Map.of("items", List.of(Map.of("label", "Sunscreen", "productId", "sunscreen")), "summary", "Beach essentials")
        );
        ResponseEntity<Map> saved = rest.exchange("/kits", HttpMethod.POST, new HttpEntity<>(saveBody, AUTH_HEADERS), Map.class);
        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(saved.getBody()).containsEntry("id", "my-beach-trip");

        ResponseEntity<List> list = rest.exchange("/kits", HttpMethod.GET, new HttpEntity<>(AUTH_HEADERS), List.class);
        assertThat(list.getBody()).hasSize(1);

        ResponseEntity<Map> renamed = rest.exchange("/kits/my-beach-trip", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("name", "Beach Trip 2026"), AUTH_HEADERS), Map.class);
        assertThat(renamed.getBody()).containsEntry("name", "Beach Trip 2026");

        rest.exchange("/kits/my-beach-trip", HttpMethod.DELETE, new HttpEntity<>(AUTH_HEADERS), Void.class);
        ResponseEntity<List> afterDelete = rest.exchange("/kits", HttpMethod.GET, new HttpEntity<>(AUTH_HEADERS), List.class);
        assertThat(afterDelete.getBody()).isEmpty();
    }
}
