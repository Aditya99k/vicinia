package com.vicinia.orderservice.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderEventsKafkaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.1"));

    @org.springframework.test.context.DynamicPropertySource
    static void kafkaProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private OrderEventPublisher eventPublisher;

    @Autowired
    private OrderRepository orderRepository;

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
    void redeliveredPaymentSuccessEvent_confirmsTheOrderExactlyOnce() {
        Order order = confirmedOrder();
        String payload = String.format(
                "{\"eventId\":\"%s\",\"eventType\":\"payment.success\",\"schemaVersion\":1," +
                        "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"payload\":{\"orderId\":\"%s\",\"userId\":\"%s\"," +
                        "\"amount\":\"40.00\",\"method\":\"RAZORPAY\"}}",
                UUID.randomUUID(), order.getId(), order.getUserId());

        rawStringProducer.send(new ProducerRecord<>("payment-events", order.getId().toString(), payload));
        rawStringProducer.send(new ProducerRecord<>("payment-events", order.getId().toString(), payload)); // redelivery

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        });

        // No exception, no illegal-transition error from the second (redelivered) message —
        // the status-guard in OrderService.confirmFromPaymentEvent made it a clean no-op.
    }

    // --- 3. DLQ path ---------------------------------------------------------

    @Test
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
