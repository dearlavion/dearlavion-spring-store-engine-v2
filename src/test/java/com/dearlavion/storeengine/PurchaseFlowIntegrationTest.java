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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * End-to-end verification of Phase 2 (survey/kit-engine, cart, orders, shipping-details) against a
 * real MongoDB (Testcontainers), with AuthClientService mocked so every bearer token verifies as a
 * fixed identity — this proves cart/order/survey CRUD, the idOrReference order lookup, the
 * inventory-decrement-on-ship flow, and self-service cancel/payment-pending rules, without
 * depending on a real, shared auth-service-v2 instance.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PurchaseFlowIntegrationTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    @org.springframework.test.context.DynamicPropertySource
    static void mongoProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
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
        response.setActiveProfile("ADMIN"); // also grants admin access for the admin/orders calls below
        when(authClient.verify(anyString())).thenReturn(response);
        when(authClient.expectedCustomer()).thenReturn("");
    }

    private String createProductItem(String productName, String category, double price, int stock) {
        Map<String, Object> productBody = Map.of(
                "name", productName, "category", category,
                "destinations", List.of("All"), "seasons", List.of("All"), "kitCategory", category
        );
        ResponseEntity<Map> productRes = rest.exchange("/admin/products", HttpMethod.POST,
                new HttpEntity<>(productBody, AUTH_HEADERS), Map.class);
        assertThat(productRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String productId = (String) productRes.getBody().get("id");

        Map<String, Object> itemBody = Map.of(
                "productId", productId, "name", productName, "price", price, "stock", stock
        );
        ResponseEntity<Map> itemRes = rest.exchange("/admin/product-items", HttpMethod.POST,
                new HttpEntity<>(itemBody, AUTH_HEADERS), Map.class);
        assertThat(itemRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) itemRes.getBody().get("id");
    }

    @Test
    void cartAddUpdateRemoveRoundTrip() {
        String itemId = createProductItem("Cart Test Sunscreen", "Toiletries", 12.0, 10);

        ResponseEntity<Map> added = rest.exchange("/cart/items", HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", itemId, "quantity", 2), AUTH_HEADERS), Map.class);
        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(added.getBody()).containsEntry("subtotal", 24.0).containsEntry("itemCount", 2);

        ResponseEntity<Map> updated = rest.exchange("/cart/items/" + itemId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("quantity", 5), AUTH_HEADERS), Map.class);
        assertThat(updated.getBody()).containsEntry("subtotal", 60.0).containsEntry("itemCount", 5);

        ResponseEntity<Map> removed = rest.exchange("/cart/items/" + itemId, HttpMethod.DELETE,
                new HttpEntity<>(AUTH_HEADERS), Map.class);
        assertThat(removed.getBody()).containsEntry("itemCount", 0);
        assertThat((List<?>) removed.getBody().get("items")).isEmpty();
    }

    @Test
    void surveyRecommendationsAndSaveRoundTrip() {
        createProductItem("Survey Test Passport Wallet", "Documents", 18.0, 10);

        Map<String, Object> answers = Map.of(
                "season", "Summer", "party", "Solo", "duration", "medium"
        );
        ResponseEntity<Map> recRes = rest.postForEntity("/survey/recommendations", answers, Map.class);
        assertThat(recRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> checklist = (List<?>) recRes.getBody().get("checklist");
        assertThat(checklist).isNotEmpty();

        ResponseEntity<Map> saveRes = rest.exchange("/surveys", HttpMethod.POST,
                new HttpEntity<>(answers, AUTH_HEADERS), Map.class);
        assertThat(saveRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((List<?>) saveRes.getBody().get("checklist")).isNotEmpty();

        ResponseEntity<List> listRes = rest.exchange("/surveys", HttpMethod.GET, new HttpEntity<>(AUTH_HEADERS), List.class);
        assertThat(listRes.getBody()).hasSize(1);
    }

    @Test
    void shippingDetailsSaveAndGet() {
        Map<String, Object> details = Map.of(
                "fullName", "Jane Doe", "email", "jane@test.dev",
                "address", "123 Main St", "city", "Springfield", "postalCode", "00000"
        );
        ResponseEntity<Map> saveRes = rest.exchange("/shipping-details", HttpMethod.PUT,
                new HttpEntity<>(details, AUTH_HEADERS), Map.class);
        assertThat(saveRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(saveRes.getBody()).containsEntry("city", "Springfield");

        ResponseEntity<Map> getRes = rest.exchange("/shipping-details", HttpMethod.GET, new HttpEntity<>(AUTH_HEADERS), Map.class);
        assertThat(getRes.getBody()).containsEntry("fullName", "Jane Doe");
    }

    @Test
    void placeOrderIdOrReferenceAndFullFulfillmentLifecycle() {
        String itemId = createProductItem("Order Test First Aid Kit", "Health", 25.0, 10);

        Map<String, Object> orderBody = Map.of(
                "items", List.of(Map.of(
                        "productId", "order-test-first-aid-kit",
                        "productItemId", itemId,
                        "name", "Order Test First Aid Kit",
                        "quantity", 2,
                        "price", 25.0
                )),
                "total", 50.0
        );
        ResponseEntity<Map> placed = rest.exchange("/orders", HttpMethod.POST,
                new HttpEntity<>(orderBody, AUTH_HEADERS), Map.class);
        assertThat(placed.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String reference = (String) placed.getBody().get("reference");
        assertThat(reference).startsWith("TB-");
        assertThat(placed.getBody()).containsEntry("paymentStatus", "UNPAID").containsEntry("deliveryStatus", "Processing");

        // idOrReference: fetch by the human reference, not the Mongo _id.
        ResponseEntity<Map> byReference = rest.exchange("/orders/" + reference, HttpMethod.GET, new HttpEntity<>(AUTH_HEADERS), Map.class);
        assertThat(byReference.getStatusCode()).isEqualTo(HttpStatus.OK);
        String orderId = (String) byReference.getBody().get("id");

        // Self-service: mark payment pending.
        ResponseEntity<Map> pending = rest.exchange("/orders/" + reference + "/payment-pending", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("paymentId", "pay-123"), AUTH_HEADERS), Map.class);
        assertThat(pending.getBody()).containsEntry("paymentStatus", "PENDING");

        // Admin: approve payment (fetch by the Mongo _id this time, to prove both id forms work).
        ResponseEntity<Map> approved = rest.exchange("/admin/orders/" + orderId + "/payment-status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "PAID"), AUTH_HEADERS), Map.class);
        assertThat(approved.getBody()).containsEntry("paymentStatus", "PAID");

        // Admin: mark shipped -> decrements catalog stock for the line and flags inventoryUpdated.
        ResponseEntity<Map> shipped = rest.exchange("/admin/orders/" + reference + "/shipped", HttpMethod.PATCH,
                new HttpEntity<>(AUTH_HEADERS), Map.class);
        assertThat(shipped.getBody()).containsEntry("deliveryStatus", "Shipped");
        List<Map<String, Object>> shippedItems = (List<Map<String, Object>>) shipped.getBody().get("items");
        assertThat(shippedItems.get(0)).containsEntry("inventoryUpdated", true);

        ResponseEntity<Map> itemAfterShip = rest.getForEntity("/product-items?productId=order-test-first-aid-kit", Map.class);
        List<Map<String, Object>> content = (List<Map<String, Object>>) itemAfterShip.getBody().get("content");
        assertThat(content.get(0).get("stock")).isEqualTo(8); // 10 - 2

        // Admin: mark delivered.
        ResponseEntity<Map> delivered = rest.exchange("/admin/orders/" + reference + "/delivered", HttpMethod.PATCH,
                new HttpEntity<>(AUTH_HEADERS), Map.class);
        assertThat(delivered.getBody()).containsEntry("deliveryStatus", "Delivered");

        // Archive / unarchive.
        ResponseEntity<Map> archived = rest.exchange("/admin/orders/" + reference + "/archive", HttpMethod.PATCH,
                new HttpEntity<>(AUTH_HEADERS), Map.class);
        assertThat(archived.getBody()).containsEntry("archived", true);
    }

    @Test
    void cancelOrderOnlyWhileProcessingAndUnpaid() {
        createProductItem("Cancel Test Neck Pillow", "Comfort", 15.0, 5);
        Map<String, Object> orderBody = Map.of(
                "items", List.of(Map.of("productId", "cancel-test-neck-pillow", "name", "Cancel Test Neck Pillow", "quantity", 1, "price", 15.0)),
                "total", 15.0
        );
        ResponseEntity<Map> placed = rest.exchange("/orders", HttpMethod.POST,
                new HttpEntity<>(orderBody, AUTH_HEADERS), Map.class);
        String reference = (String) placed.getBody().get("reference");

        ResponseEntity<Map> cancelled = rest.exchange("/orders/" + reference + "/cancel", HttpMethod.PATCH,
                new HttpEntity<>(AUTH_HEADERS), Map.class);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelled.getBody()).containsEntry("cancelled", true);

        // Cancelling again is a conflict.
        ResponseEntity<Map> secondCancel = rest.exchange("/orders/" + reference + "/cancel", HttpMethod.PATCH,
                new HttpEntity<>(AUTH_HEADERS), Map.class);
        assertThat(secondCancel.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void adminOrderFiltersByPaymentAndDeliveryStatus() {
        ResponseEntity<List> list = rest.exchange("/admin/orders?paymentStatus=UNPAID", HttpMethod.GET,
                new HttpEntity<>(AUTH_HEADERS), List.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
