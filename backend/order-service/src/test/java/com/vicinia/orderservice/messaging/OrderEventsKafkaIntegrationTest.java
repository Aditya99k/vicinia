package com.vicinia.orderservice.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vicinia.orderservice.client.InventoryClient;
import com.vicinia.orderservice.domain.Order;
import com.vicinia.orderservice.domain.OrderItem;
import com.vicinia.orderservice.domain.OrderStatus;
import com.vicinia.orderservice.repository.OrderRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Stage 17's Kafka test — one thorough class covering producer shape,
 * idempotent consumer, and the DLQ path (ARCHITECTURE.md §17), against
 * real Postgres + real Kafka via Testcontainers, rather than replicating
 * this same pattern across every one of the 9 Kafka-consuming services.
 * order-service is the richest candidate: it both publishes (order.
 * confirmed) and consumes (payment-events, for the Razorpay path).
 *
 * <p>The 3 tests share one Spring context — one {@code payment-events}
 * consumer group, one listener — so they aren't isolated from each other:
 * the malformed-message test's poison record needs its full 4-attempt,
 * ~14s exponential backoff (2s/4s/8s) before landing on the DLT, which
 * blocks that topic-partition's consumer the whole time. Explicit
 * ordering keeps it running last, after the redelivery test's own
 * message has already been consumed — found the hard way in CI, where
 * default (effectively unordered) method execution let the malformed
 * message go first and starved the redelivery test past its timeout.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
class OrderEventsKafkaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    // The native apache/kafka image's startup log format doesn't match this
    // Testcontainers version's wait strategy (calibrated for cp-kafka's
    // ZooKeeper-era "[KafkaServer id=N] started" line) — asCompatibleSubstituteFor
    // only bypasses the image-name check, not the wait condition, so the
    // container still timed out. confluentinc/cp-kafka is what this class
    // actually supports.
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @org.springframework.test.context.DynamicPropertySource
    static void kafkaProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private OrderEventPublisher eventPublisher;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * OrderService.confirmFromPaymentEvent/failFromPaymentEvent call out to
     * inventory-service over real HTTP (InventoryClient) — nothing this
     * test starts. Without this, the redelivery test's own payment.success
     * message throws on every @RetryableTopic attempt (connection failure,
     * no inventory-service instance), lands on the DLT after ~14s of
     * retries, and never confirms — found via a genuine CI run where the
     * order stayed at PAYMENT_PENDING and the DLQ test then saw more than
     * the one record it expected.
     */
    @MockBean
    private InventoryClient inventoryClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KafkaTemplate<String, String> rawStringProducer;
    private Consumer<String, String> rawStringConsumer;

    @BeforeEach
    void setUpRawKafkaClients() {
        Map<String, Object> producerProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(producerProps);
        rawStringProducer = new KafkaTemplate<>(pf);

        rawStringConsumer = new DefaultKafkaConsumerFactory<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-observer-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(), new StringDeserializer()).createConsumer();
    }

    private Order confirmedOrder() {
        Order order = new Order(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("40.00"));
        order.addItem(new OrderItem(UUID.randomUUID(), "product-1", "Test Product", new BigDecimal("40.00"), 1));
        order.transitionTo(OrderStatus.PAYMENT_PENDING);
        return orderRepository.saveAndFlush(order);
    }

    // --- 1. Producer shape -------------------------------------------------

    @Test
    @org.junit.jupiter.api.Order(1)
    void publishConfirmed_putsARealOrderConfirmedEnvelopeOnOrderEvents() throws Exception {
        rawStringConsumer.subscribe(java.util.List.of("order-events"));
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        eventPublisher.publishConfirmed(orderId, userId, merchantId);

        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(rawStringConsumer, "order-events", Duration.ofSeconds(15));
        JsonNode envelope = objectMapper.readTree(record.value());

        assertThat(envelope.get("eventType").asText()).isEqualTo("order.confirmed");
        assertThat(envelope.get("eventId").asText()).isNotBlank();
        assertThat(envelope.get("payload").get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(envelope.get("payload").get("userId").asText()).isEqualTo(userId.toString());
        assertThat(envelope.get("payload").get("merchantId").asText()).isEqualTo(merchantId.toString());
    }

    // --- 2. Idempotent consumer ---------------------------------------------

    @Test
    @org.junit.jupiter.api.Order(2)
    void redeliveredPaymentSuccessEvent_confirmsTheOrderExactlyOnce() {
        Order order = confirmedOrder();
        String payload = String.format(
                "{\"eventId\":\"%s\",\"eventType\":\"payment.success\",\"schemaVersion\":1," +
                        "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"payload\":{\"orderId\":\"%s\",\"userId\":\"%s\"," +
                        "\"amount\":\"40.00\",\"method\":\"RAZORPAY\"}}",
                UUID.randomUUID(), order.getId(), order.getUserId());

        rawStringProducer.send(new ProducerRecord<>("payment-events", order.getId().toString(), payload));
        rawStringProducer.send(new ProducerRecord<>("payment-events", order.getId().toString(), payload)); // redelivery

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        });

        // The redelivered (second) message reaches the listener too — this proves it was a
        // clean no-op (the status-guard in OrderService.confirmFromPaymentEvent), not that it
        // never arrived.
        org.mockito.Mockito.verify(inventoryClient, org.mockito.Mockito.timeout(5000).times(1)).confirm(order.getId());
    }

    // --- 3. DLQ path ---------------------------------------------------------

    @Test
    @org.junit.jupiter.api.Order(3)
    void aMalformedPaymentEvent_landsOnTheServiceQualifiedDlt() {
        rawStringConsumer.subscribe(java.util.List.of("payment-events-order-service-dlt"));
        String malformed = "{\"eventId\":\"bad-1\",\"eventType\":\"payment.success\",\"schemaVersion\":1," +
                "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"payload\":{\"orderId\":\"not-a-valid-uuid\"," +
                "\"userId\":\"also-not-a-uuid\",\"amount\":\"40.00\",\"method\":\"RAZORPAY\"}}";

        rawStringProducer.send(new ProducerRecord<>("payment-events", "malformed-test-key", malformed));

        ConsumerRecord<String, String> dltRecord = KafkaTestUtils.getSingleRecord(
                rawStringConsumer, "payment-events-order-service-dlt", Duration.ofSeconds(30));

        assertThat(dltRecord.value()).contains("\"eventId\":\"bad-1\"");
    }
}
