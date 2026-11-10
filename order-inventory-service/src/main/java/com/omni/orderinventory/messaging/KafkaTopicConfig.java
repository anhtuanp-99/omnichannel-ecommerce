package com.omni.orderinventory.messaging;

import com.omni.common.constant.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Khai báo topic idempotent — đảm bảo topic luôn sẵn sàng trước khi service nhận bản tin. */
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

    @Bean
    public NewTopic orderEventsDlt() {
        return TopicBuilder.name(KafkaTopics.ORDER_EVENTS + ".DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentEventsDlt() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_EVENTS + ".DLT").partitions(3).replicas(1).build();
    }
}