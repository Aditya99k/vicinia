package com.vicinia.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Stage 17's "one full e2e test" (BUILD_TRACKER.md): checkout -> confirm
 * -> merchant accept -> delivery -> deliver -> settlement, driven entirely
 * over HTTP against an already-running stack (api-gateway on :8080),
 * automating the exact manual curl-driven flow this project's own testing
 * has repeated by hand at the end of Stages 11 through 16.
 *
 * <p>Requires the full stack running first — locally via ./start-infra.sh,
 * or in CI via the e2e-test workflow's own equivalent steps (this module
 * intentionally starts nothing itself, so it exercises the system exactly
 * as a real client would, the same posture every prior stage's live
 * verification took).
 */
class FullOrderLifecycleE2ETest {

    private static final String BASE_URL = System.getProperty("e2e.baseUrl", "http://localhost:8080");
    private static final String POSTGRES_URL = System.getProperty("e2e.postgresUrl", "jdbc:postgresql://localhost:5432/auth_db");
    private static final String POSTGRES_USER = System.getProperty("e2e.postgresUser", System.getenv().getOrDefault("POSTGRES_USER", "vicinia"));
    private static final String POSTGRES_PASSWORD = System.getProperty("e2e.postgresPassword", System.getenv().getOrDefault("POSTGRES_PASSWORD", "vicinia"));

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void checkoutThroughSettlement() throws Exception {
        // Nearest-partner assignment (PartnerGeoService, Redis GEO) picks whoever's
        // closest; a leftover partner left "online" by an earlier interrupted run
        // would otherwise win the race over this run's own partner. Clear the set
        // rather than track every prior test partner's id.
        flushOnlineDeliveryPartners();

        long ts = System.currentTimeMillis();

        // --- Signup: customer, merchant, delivery partner, and a soon-to-be admin ---
        JsonNode customer = signup("e2e-cust-" + ts + "@example.com", "CUSTOMER");
        String customerToken = customer.get("accessToken").asText();
        String customerUserId = customer.get("userId").asText();

        JsonNode merchantAcct = signup("e2e-merch-" + ts + "@example.com", "MERCHANT");
        String merchantToken = merchantAcct.get("accessToken").asText();

        JsonNode partnerAcct = signup("e2e-partner-" + ts + "@example.com", "DELIVERY_PARTNER");
        String partnerToken = partnerAcct.get("accessToken").asText();

        JsonNode adminSignup = signup("e2e-admin-" + ts + "@example.com", "CUSTOMER");
        String adminUserId = adminSignup.get("userId").asText();
        promoteToAdmin(adminUserId);
        String adminToken = login("e2e-admin-" + ts + "@example.com").get("accessToken").asText();

        // --- Merchant onboarding ---
        JsonNode application = post("/api/merchants/apply", merchantToken, """
                {"storeName":"E2E Test Store","description":"e2e","addressLine1":"1 Test Rd",
                 "city":"Bangalore","state":"KA","pincode":"560001",
                 "documents":[{"documentType":"GST","referenceUrl":"https://example.com/doc.pdf"}]}
                """);
        String merchantId = application.get("id").asText();
        post("/api/merchants/admin/" + merchantId + "/approve", adminToken, null);
        put("/api/merchants/me", merchantToken, """
                {"storeName":"E2E Test Store","description":"e2e","addressLine1":"1 Test Rd",
                 "city":"Bangalore","state":"KA","pincode":"560001",
                 "latitude":12.9352,"longitude":77.6146,"deliveryRadiusKm":10}
                """);
        put("/api/merchants/me/hours", merchantToken, "{\"openTime\":\"09:00:00\",\"closeTime\":\"23:00:00\"}");
        post("/api/merchants/me/go-live", merchantToken, null);

        // --- Catalog + inventory ---
        JsonNode product = post("/api/catalog/admin/products", adminToken, """
                {"name":"E2E Test Product","brand":"TestBrand","category":"Snacks",
                 "description":"e2e","images":[],"attributes":{}}
                """);
        String productId = product.get("id").asText();

        JsonNode listing = post("/api/inventory/listings", merchantToken,
                "{\"productId\":\"" + productId + "\",\"price\":40.00,\"availableStock\":10}");
        String listingId = listing.get("id").asText();

        // --- Delivery partner goes online near the merchant ---
        post("/api/delivery/partners/online", partnerToken, "{\"latitude\":12.9358,\"longitude\":77.6150}");

        // --- Checkout: wallet top-up, cart, place order ---
        post("/api/payments/wallet/topup", customerToken, "{\"amount\":500.00}");
        post("/api/cart/items", customerToken, "{\"listingId\":\"" + listingId + "\",\"quantity\":1}");
        JsonNode order = post("/api/orders", customerToken, "{\"paymentMethod\":\"WALLET\"}");
        String orderId = order.get("id").asText();
        assertThat(order.get("status").asText()).isEqualTo("CONFIRMED");

        // --- Merchant fulfillment ---
        // order.confirmed -> merchant-service's OrderConfirmedConsumer creates the
        // MerchantOrderTask asynchronously; wait for it to land before accepting.
        await("merchant order task created from order.confirmed").atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            JsonNode pending = get("/api/merchants/orders/pending", merchantToken);
            assertThat(pending).anySatisfy(t -> assertThat(t.get("orderId").asText()).isEqualTo(orderId));
        });
        post("/api/merchants/orders/" + orderId + "/accept", merchantToken, null);
        JsonNode ready = post("/api/merchants/orders/" + orderId + "/ready", merchantToken, null);
        assertThat(ready.get("status").asText()).isEqualTo("READY");

        // --- Delivery assignment + fulfillment ---
        await("delivery task assigned via order.ready -> Redis GEO search").atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            JsonNode current = get("/api/orders/" + orderId, customerToken);
            assertThat(current.get("status").asText()).isEqualTo("DELIVERY_ASSIGNED");
        });
        post("/api/delivery/tasks/" + orderId + "/accept", partnerToken, null);
        post("/api/delivery/tasks/" + orderId + "/picked-up", partnerToken, null);
        post("/api/delivery/tasks/" + orderId + "/delivered", partnerToken, null);

        JsonNode delivered = get("/api/orders/" + orderId, customerToken);
        assertThat(delivered.get("status").asText()).isEqualTo("DELIVERED");

        // --- Settlement ---
        await("settlement entry created from order.delivered").atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            JsonNode entries = get("/api/settlements/mine", merchantToken);
            assertThat(entries.isArray()).isTrue();
            assertThat(entries).anySatisfy(e -> assertThat(e.get("orderId").asText()).isEqualTo(orderId));
        });

        JsonNode entries = get("/api/settlements/mine", merchantToken);
        JsonNode thisOrdersEntry = null;
        for (JsonNode e : entries) {
            if (e.get("orderId").asText().equals(orderId)) {
                thisOrdersEntry = e;
                break;
            }
        }
        assertThat(thisOrdersEntry).isNotNull();
        assertThat(thisOrdersEntry.get("gross").asDouble()).isEqualTo(40.00);
        assertThat(thisOrdersEntry.get("commission").asDouble()).isEqualTo(2.00);
        assertThat(thisOrdersEntry.get("net").asDouble()).isEqualTo(38.00);

        post("/api/settlements/admin/payouts/run-batch", adminToken, null);

        await("payout reaches PAID via the simulated processor").atMost(Duration.ofSeconds(90)).pollInterval(Duration.ofSeconds(5)).untilAsserted(() -> {
            post("/api/settlements/admin/payouts/run-processor", adminToken, null);
            JsonNode payouts = get("/api/settlements/payouts/mine", merchantToken);
            boolean paid = false;
            for (JsonNode p : payouts) {
                if ("PAID".equals(p.get("status").asText())) {
                    paid = true;
                }
            }
            assertThat(paid).isTrue();
        });

        // Silence the unused-variable warning without pretending customerUserId matters beyond documenting who placed the order.
        assertThat(customerUserId).isNotBlank();
    }

    // --- HTTP helpers -----------------------------------------------------

    private JsonNode signup(String email, String role) throws Exception {
        return post("/api/auth/signup", null, "{\"email\":\"" + email + "\",\"password\":\"TestPass123\",\"role\":\"" + role + "\"}");
    }

    private JsonNode login(String email) throws Exception {
        return post("/api/auth/login", null, "{\"email\":\"" + email + "\",\"password\":\"TestPass123\"}");
    }

    private JsonNode get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path)).GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("GET %s -> %s", path, response.body()).isBetween(200, 299);
        return json.readTree(response.body());
    }

    private JsonNode post(String path, String token, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("POST %s -> %s", path, response.body()).isBetween(200, 299);
        String responseBody = response.body();
        return responseBody == null || responseBody.isBlank() ? json.createObjectNode() : json.readTree(responseBody);
    }

    private JsonNode put(String path, String token, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("PUT %s -> %s", path, response.body()).isBetween(200, 299);
        return json.readTree(response.body());
    }

    /** Raw RESP DEL against Redis's delivery-partner GEO set — no client library needed for one command. */
    private void flushOnlineDeliveryPartners() throws Exception {
        String redisHost = System.getProperty("e2e.redisHost", "localhost");
        int redisPort = Integer.getInteger("e2e.redisPort", 6379);
        String key = "delivery:partners:online";
        String command = "*2\r\n$3\r\nDEL\r\n$" + key.length() + "\r\n" + key + "\r\n";
        try (java.net.Socket socket = new java.net.Socket(redisHost, redisPort)) {
            socket.getOutputStream().write(command.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            socket.getInputStream().read(new byte[64]);
        }
    }

    /** Automates the exact manual SQL step this project's own testing has used since Stage 3 — see BUILD_TRACKER.md's documented admin-bootstrap gap. */
    private void promoteToAdmin(String userId) throws Exception {
        try (Connection conn = DriverManager.getConnection(POSTGRES_URL, POSTGRES_USER, POSTGRES_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO user_roles (user_id, role_id) SELECT '" + userId + "', id FROM roles WHERE name='ADMIN' ON CONFLICT DO NOTHING");
            stmt.execute("DELETE FROM user_roles WHERE user_id='" + userId + "' AND role_id IN (SELECT id FROM roles WHERE name='CUSTOMER')");
        }
    }
}
