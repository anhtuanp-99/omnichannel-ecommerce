package com.omni.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omni.payment.common.PaymentConstants;
import com.omni.payment.payment.Payment;
import com.omni.payment.payment.PaymentRepository;
import com.omni.payment.payment.PaymentStatus;
import com.omni.payment.webhook.HmacService;
import com.omni.payment.webhook.dto.WebhookPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookIdempotencyTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private PaymentRepository paymentRepo;
    @Autowired private HmacService hmacService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE payment_refunds, webhook_events, payments, outbox_events, processed_events CASCADE;");
    }

    @Test
    void duplicateWebhook_isHandledIdempotently() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        // 1. Tạo sẵn bản ghi Payment ở trạng thái PENDING
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setOrderId(orderId);
        payment.setAmount(100_000L);
        payment.setRefundedAmount(0L);
        payment.setCurrency("VND");
        payment.setProvider("MOCK");
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepo.save(payment);

        WebhookPayload payload = new WebhookPayload(
                UUID.randomUUID().toString(),
                PaymentConstants.PROVIDER_MOCK,
                "mock-txn-idempotent-01",
                paymentId.toString(),
                orderId.toString(),
                100_000L,
                "VND",
                "SUCCEEDED",
                null,
                Instant.now()
        );

        String rawBody = objectMapper.writeValueAsString(payload);
        String validSignature = hmacService.sign(rawBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(PaymentConstants.HMAC_HEADER, validSignature);

        HttpEntity<String> requestEntity = new HttpEntity<>(rawBody, headers);

        // 2. Bắn Webhook lần 1 -> Thành công (200 OK)
        ResponseEntity<Void> resp1 = restTemplate.exchange("/api/v1/payments/webhook", HttpMethod.POST, requestEntity, Void.class);
        assertThat(resp1.getStatusCode().is2xxSuccessful()).isTrue();

        // 3. Bắn Webhook lần 2 cùng providerTxnId -> Vẫn trả về 200 OK nhưng không xử lý lại
        ResponseEntity<Void> resp2 = restTemplate.exchange("/api/v1/payments/webhook", HttpMethod.POST, requestEntity, Void.class);
        assertThat(resp2.getStatusCode().is2xxSuccessful()).isTrue();

        // 4. Kiểm tra trạng thái DB: Phải là SUCCEEDED và không có biến động sai lệch
        Payment updatedPayment = paymentRepo.findById(paymentId).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(updatedPayment.getRefundedAmount()).isZero();
    }

    @Test
    void invalidSignature_returnsUnauthorized() throws Exception {
        WebhookPayload payload = new WebhookPayload(
                UUID.randomUUID().toString(),
                PaymentConstants.PROVIDER_MOCK,
                "mock-txn-invalid-sig",
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                100_000L,
                "VND",
                "SUCCEEDED",
                null,
                Instant.now()
        );

        String rawBody = objectMapper.writeValueAsString(payload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(PaymentConstants.HMAC_HEADER, "sha256=invalidhexsignature1234567890");

        HttpEntity<String> requestEntity = new HttpEntity<>(rawBody, headers);

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/payments/webhook", HttpMethod.POST, requestEntity, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }
}