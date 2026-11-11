package com.omni.notification.log;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {
    Optional<NotificationLog> findByEventId(String eventId);
}