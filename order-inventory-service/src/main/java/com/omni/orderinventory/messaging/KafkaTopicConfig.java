package com.omni.orderinventory.messaging;

import com.omni.common.constant.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Khai báo topic — đảm bảo topic tồn tại trước khi service start.
 * Với local dev, docker-compose đã tạo sẵn; beans này là idempotent.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_EVENTS).partitions(3).replicas(1).build();
    }
}