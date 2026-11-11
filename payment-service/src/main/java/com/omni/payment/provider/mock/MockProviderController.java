package com.omni.payment.provider.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omni.payment.common.PaymentConstants;
import com.omni.payment.webhook.HmacService;
import com.omni.payment.webhook.dto.WebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Controller giả lập trang Checkout của bên thứ 3 (Mock Provider)
 * và tự động bắn Webhook về hệ thống của chúng ta.
 */
@RestController
@RequestMapping("/api/v1/mock-provider")
public class MockProviderController {

    private static final Logger log = LoggerFactory.getLogger(MockProviderController.class);

    private final HmacService hmac;
    private final ObjectMapper om;
    private final MockProviderProperties props;
    private final RestTemplate rest = new RestTemplate();

    public MockProviderController(HmacService hmac, ObjectMapper om, MockProviderProperties props) {
        this.hmac = hmac;
        this.om = om;
        this.props = props;
    }

    @GetMapping("/checkout")
    public ResponseEntity<?> checkout(
            @RequestParam String paymentId,
            @RequestParam String providerRef,
            @RequestParam long amount,
            @RequestParam String currency) throws InterruptedException {

        // Giả lập thời gian khách thao tác nhập thẻ mất 1.5 giây
        Thread.sleep(1500);
        return simulate(paymentId, providerRef, amount, currency, "SUCCEEDED", null);
    }

    @GetMapping("/checkout/fail")
    public ResponseEntity<?> fail(
            @RequestParam String paymentId,
            @RequestParam String providerRef,
            @RequestParam long amount,
            @RequestParam String currency) throws InterruptedException {

        Thread.sleep(1000);
        return simulate(paymentId, providerRef, amount, currency, "FAILED", "PAYMENT_DECLINED");
    }

    private ResponseEntity<?> simulate(String paymentId, String providerRef,
                                       long amount, String currency,
                                       String status, String failureCategory) {
        try {
            // Tạo payload giả lập
            WebhookPayload payload = new WebhookPayload(
                    UUID.randomUUID().toString(),
                    PaymentConstants.PROVIDER_MOCK,
                    providerRef,
                    paymentId,
                    "order-mock",
                    amount,
                    currency,
                    status,
                    failureCategory,
                    Instant.now()
            );

            // Ký HMAC để giả lập bảo mật
            String rawBody = om.writeValueAsString(payload);
            String signature = hmac.sign(rawBody);

            // Chuẩn bị request gửi về hệ thống thật
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(PaymentConstants.HMAC_HEADER, signature);

            HttpEntity<String> req = new HttpEntity<>(rawBody, headers);
            ResponseEntity<String> resp = rest.exchange(props.getWebhookUrl(), HttpMethod.POST, req, String.class);

            log.info("Mock webhook đã gửi paymentId={} status={} statusCode={}", paymentId, status, resp.getStatusCode());

            return ResponseEntity.ok(Map.of(
                    "message", "Thanh toán " + status + " — vui lòng quay lại app",
                    "paymentId", paymentId,
                    "status", status
            ));
        } catch (Exception e) {
            log.error("Lỗi khi bắn Mock webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}