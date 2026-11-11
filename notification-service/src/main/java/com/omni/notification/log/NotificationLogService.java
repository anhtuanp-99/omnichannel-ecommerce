package com.omni.notification.log;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class NotificationLogService {

    private final NotificationLogRepository repo;

    public NotificationLogService(NotificationLogRepository repo) {
        this.repo = repo;
    }

    public Optional<NotificationLog> findByEventId(String eventId) {
        return repo.findByEventId(eventId);
    }

    public NotificationLog createQueued(String eventId, String eventType, String recipient, String templateCode) {
        NotificationLog log = new NotificationLog();
        log.setEventId(eventId);
        log.setEventType(eventType);
        log.setChannel(NotificationChannel.EMAIL.name());
        log.setRecipientMasked(maskEmail(recipient));
        log.setTemplateCode(templateCode);
        log.setStatus(NotificationStatus.QUEUED.name());
        log.setQueuedAt(Instant.now());
        return repo.save(log);
    }

    public void markSent(NotificationLog log, String providerMessageId) {
        log.setStatus(NotificationStatus.SENT.name());
        log.setProviderMessageId(providerMessageId);
        log.setSentAt(Instant.now());
        repo.save(log);
    }

    public void markFailed(NotificationLog log, String reason) {
        log.setStatus(NotificationStatus.FAILED.name());
        log.setFailureReason(reason);
        repo.save(log);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) return local.charAt(0) + "***" + domain;
        return local.charAt(0) + "***" + domain;
    }
}