package com.omni.payment.webhook;

import com.omni.payment.common.PaymentConstants;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/webhook")
public class PaymentWebhookController {

    private final WebhookService service;

    public PaymentWebhookController(WebhookService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody String rawBody,
                                        @RequestHeader(value = PaymentConstants.HMAC_HEADER, required = false) String sig) {
        service.handle(rawBody, sig);
        return ResponseEntity.ok().build();
    }
}