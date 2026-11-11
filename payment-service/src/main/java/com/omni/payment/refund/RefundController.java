package com.omni.payment.refund;

import com.omni.payment.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasAnyRole('ADMIN','FINANCE_STAFF')")
public class RefundController {

    private final RefundService service;

    public RefundController(RefundService service) {
        this.service = service;
    }

    @PostMapping("/{paymentId}/refund")
    public PaymentRefund refund(@CurrentUser UUID actorId, @PathVariable UUID paymentId,
                                @Valid @RequestBody RefundRequest req) {
        return service.refund(paymentId, req.amount(), req.reason(), actorId);
    }

    public record RefundRequest(@Min(1) long amount, @NotBlank String reason) {}
}