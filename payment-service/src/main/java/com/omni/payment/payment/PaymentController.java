package com.omni.payment.payment;

import com.omni.payment.payment.dto.PayResponse;
import com.omni.payment.payment.dto.PaymentResponse;
import com.omni.payment.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public PaymentResponse get(@CurrentUser UUID userId, @PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public PaymentResponse getByOrder(@CurrentUser UUID userId, @PathVariable UUID orderId) {
        return service.getByOrderId(orderId);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    public PayResponse pay(@CurrentUser UUID userId, @PathVariable UUID id) {
        return service.initiatePayment(id);
    }
}