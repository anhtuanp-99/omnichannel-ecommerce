package com.omni.payment.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    boolean existsByProviderAndProviderTxnId(String provider, String providerTxnId);
}