package com.omni.orderinventory.outbox;

import com.omni.common.constant.KafkaTopics;
import com.omni.common.event.EventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Scheduled publisher — đọc outbox_events PENDING, gửi Kafka.
 * Retry max N lần → FAILED.
 * Kafka producer idempotent + key = aggregateId → ordering per order.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repo;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper om;
    private final int batchSize;
    private final int maxRetry;

    public OutboxPublisher(OutboxEventRepository repo, KafkaTemplate<String, Object> kafka,
                           ObjectMapper om,
                           @Value("${app.outbox.publisher.batch-size:100}") int batchSize,
                           @Value("${app.outbox.publisher.max-retry:5}") int maxRetry) {
        this.repo = repo;
        this.kafka = kafka;
        this.om = om;
        this.batchSize = batchSize;
        this.maxRetry = maxRetry;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher.interval-ms:2000}")
    @Transactional
    public void publishPending() {
        var events = repo.findPending(PageRequest.of(0, batchSize));
        for (OutboxEvent e : events) {
            try {
                EventEnvelope<Object> envelope = new EventEnvelope<>(
                        e.getEventId().toString(),
                        e.getEventType(),
                        e.getEventVersion(),
                        e.getCreatedAt(),
                        correlationIdOf(e),
                        "order-inventory-service",
                        e.getPayload()
                );

                String topic = topicFor(e.getEventType());
                // Key = aggregateId → tất cả event của cùng 1 order vào cùng partition
                kafka.send(topic, e.getAggregateId().toString(), envelope)
                        .get(5, TimeUnit.SECONDS);

                e.markPublished();
                log.debug("Published {} eventId={}", e.getEventType(), e.getEventId());
            } catch (Exception ex) {
                log.warn("Publish failed eventId={} retry={}: {}",
                        e.getEventId(), e.getRetryCount(), ex.getMessage());
                e.incrementRetry();
                if (e.getRetryCount() >= maxRetry) {
                    e.markFailed();
                    log.error("Event FAILED after {} retries eventId={}", maxRetry, e.getEventId());
                }
            }
        }
        // @Transactional commit → flush changes
    }

    private String topicFor(String eventType) {
        return switch (eventType) {
            case "OrderCreated", "OrderConfirmed", "OrderStatusChanged", "OrderCancelled"
                    -> KafkaTopics.ORDER_EVENTS;
            default -> KafkaTopics.ORDER_EVENTS;
        };
    }

    private String correlationIdOf(OutboxEvent e) {
        Object cid = e.getPayload().get("correlationId");
        return cid != null ? cid.toString() : e.getEventId().toString();
    }
}