package com.omni.orderinventory.order;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mã đơn: ORD-yyyyMMdd-XXXXXX (6 ký tự base36).
 * Không chứa PII. Uniqueness enforce bởi UNIQUE constraint DB.
 */
@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";  // bỏ I,O,0,1

    public String generate() {
        String date = LocalDate.now(ZoneOffset.UTC).format(DATE_FMT);
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(ALPHABET.charAt(ThreadLocalRandom.current().nextInt(ALPHABET.length())));
        }
        return "ORD-" + date + "-" + suffix;
    }
}