package com.omni.common.constant;

/** Tên topic Kafka — dùng thống nhất giữa producer và consumer. */
public final class KafkaTopics {
    public static final String ORDER_EVENTS = "order.events";
    public static final String PAYMENT_EVENTS = "payment.events";
    public static final String CATALOG_EVENTS = "catalog.events";

    // Dead Letter Topic suffix — cấu hình trong DefaultErrorHandler
    public static final String DLT_SUFFIX = ".DLT";

    private KafkaTopics() {}
}