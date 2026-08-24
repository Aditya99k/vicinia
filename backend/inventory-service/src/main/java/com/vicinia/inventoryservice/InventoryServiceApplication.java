package com.vicinia.inventoryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableKafka: first service in the project with a @KafkaListener
 * (ProductEventConsumer) — every other service so far has only produced.
 * @EnableScheduling: ReservationReaper's periodic release job (ADR 0003).
 */
@SpringBootApplication
@EnableKafka
@EnableScheduling
public class InventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
