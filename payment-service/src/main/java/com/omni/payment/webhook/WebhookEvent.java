package com.omni.payment.webhook;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_txn_id", nullable = false, length = 255)
    private String providerTxnId;

    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    @Column(name = "payment_id", columnDefinition = "uuid")
    private UUID paymentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawPayload;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public WebhookEvent() {}

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (processedAt == null) processedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderTxnId() { return providerTxnId; }
    public void setProviderTxnId(String providerTxnId) { this.providerTxnId = providerTxnId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public Map<String, Object> getRawPayload() { return rawPayload; }
    public void setRawPayload(Map<String, Object> rawPayload) { this.rawPayload = rawPayload; }
    public boolean isSignatureValid() { return signatureValid; }
    public void setSignatureValid(boolean signatureValid) { this.signatureValid = signatureValid; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}