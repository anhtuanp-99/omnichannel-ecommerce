package com.omni.payment;

import com.omni.common.constant.KafkaTopics;
import com.omni.common.event.EventEnvelope;
import com.omni.common.event.payload.OrderCreatedEvent;
import com.omni.payment.payment.Payment;
import com.omni.payment.payment.PaymentRepository;
import com.omni.payment.payment.PaymentStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired private PaymentRepository paymentRepo;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE payment_refunds, webhook_events, payments, outbox_events, processed_events CASCADE;");
    }

    @Test
    void onOrderCreated_automaticallyCreatesPendingPayment() {
        UUID orderId = UUID.randomUUID();
        long orderAmount = 500_000L;

        OrderCreatedEvent payload = new OrderCreatedEvent(
                orderId.toString(),
                UUID.randomUUID().toString(),
                BigInteger.valueOf(orderAmount),
                "VND",
                Instant.now().plusSeconds(900)
        );

        // Khởi tạo envelope qua constructor chuẩn 7 tham số
        EventEnvelope<Object> envelope = new EventEnvelope<>(
                UUID.randomUUID().toString(),
                "OrderCreated",
                "1",
                Instant.now(),
                orderId.toString(),
                "order-inventory-service",
                payload
        );

        // Bắn event lên topic order.events
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, orderId.toString(), envelope);

        // Chờ tối đa 15 giây để Consumer tiêu thụ và ghi DB
        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Payment payment = paymentRepo.findByOrderId(orderId).orElse(null);
            assertThat(payment).isNotNull();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getAmount()).isEqualTo(orderAmount);
            assertThat(payment.getCurrency()).isEqualTo("VND");
        });
    }
}