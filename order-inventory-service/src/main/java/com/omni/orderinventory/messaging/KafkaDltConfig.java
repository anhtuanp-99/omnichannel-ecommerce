package com.omni.orderinventory.messaging;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Bộ quản lý lỗi Kafka — đẩy message lỗi sang DLT sau 3 lần retry thất bại. */
@Configuration
public class KafkaDltConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // Tự động định tuyến sang topic .DLT
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));

        // Thử lại 3 lần, mỗi lần cách nhau 1s
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
    }
}