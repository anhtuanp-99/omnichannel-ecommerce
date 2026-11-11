package com.omni.payment.outbox;

import com.omni.common.constant.KafkaTopics;
import com.omni.common.event.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repo;
    private final KafkaTemplate<String, Object> kafka;
    private final int batchSize;
    private final int maxRetry;

    public OutboxPublisher(OutboxEventRepository repo,
                           KafkaTemplate<String, Object> kafka,
                           @Value("${app.outbox.publisher.batch-size:100}") int batchSize,
                           @Value("${app.outbox.publisher.max-retry:5}") int maxRetry) {
        this.repo = repo;
        this.kafka = kafka;
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
                        e.getAggregateId().toString(),
                        "payment-service",
                        e.getPayload()
                );

                String topic = KafkaTopics.PAYMENT_EVENTS;
                // Key = aggregateId đảm bảo tuần tự partition cho mỗi payment
                kafka.send(topic, e.getAggregateId().toString(), envelope)
                        .get(5, TimeUnit.SECONDS);

                e.markPublished();
                log.debug("Đã publish Outbox event {} type={}", e.getEventId(), e.getEventType());
            } catch (Exception ex) {
                log.warn("Publish outbox thất bại eventId={} lần thử {}: {}", e.getEventId(), e.getRetryCount(), ex.getMessage());
                e.incrementRetry();
                if (e.getRetryCount() >= maxRetry) {
                    e.markFailed();
                    log.error("Outbox event FAILED sau {} lần thử eventId={}", maxRetry, e.getEventId());
                }
            }
        }
    }
}