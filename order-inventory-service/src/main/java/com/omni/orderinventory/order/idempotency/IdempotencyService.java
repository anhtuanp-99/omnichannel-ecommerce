package com.omni.orderinventory.order.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repo;
    private final ObjectMapper om;
    private final int ttlHours;

    public IdempotencyService(IdempotencyRecordRepository repo, ObjectMapper om,
                              @Value("${app.idempotency.ttl-hours:24}") int ttlHours) {
        this.repo = repo;
        this.om = om;
        this.ttlHours = ttlHours;
    }

    /** Tạo mã hash SHA-256 đại diện cho payload để so sánh request */
    public String hash(Object payload) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(om.writeValueAsBytes(payload));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Hash failed", e);
        }
    }

    public Optional<IdempotencyRecord> find(String key) {
        return repo.findById(key);
    }

    /** Đánh dấu request đang được xử lý (PROCESSING) */
    public IdempotencyRecord markProcessing(String key, String requestHash) {
        IdempotencyRecord r = new IdempotencyRecord();
        r.setKey(key);
        r.setRequestHash(requestHash);
        r.setStatus(IdempotencyStatus.PROCESSING);
        r.setExpiresAt(Instant.now().plus(ttlHours, ChronoUnit.HOURS));
        return repo.saveAndFlush(r);
    }

    /** Lưu lại kết quả (JSON String) khi xử lý xong */
    public void markCompleted(IdempotencyRecord r, String responseJson) {
        r.setStatus(IdempotencyStatus.COMPLETED);
        r.setResponseJson(responseJson);
        repo.save(r);
    }

    /** Đọc lại kết quả cũ */
    public String readResponse(IdempotencyRecord r) {
        return r.getResponseJson();
    }
}