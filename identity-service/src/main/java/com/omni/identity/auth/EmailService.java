package com.omni.identity.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Gửi email trực tiếp qua Mailpit (Phase 2 — sau này có thể đẩy qua Kafka). */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mail;
    private final String from;
    private final String baseUrl;

    public EmailService(JavaMailSender mail,
                        @Value("${app.mail.from:noreply@omni-ecom.local}") String from,
                        @Value("${app.public-base-url:http://localhost:8080}") String baseUrl) {
        this.mail = mail;
        this.from = from;
        this.baseUrl = baseUrl;
    }

    public void sendVerifyEmail(String to, String token) {
        String link = baseUrl + "/api/v1/auth/verify-email?token=" + token;
        sendSimple(to, "Xác minh tài khoản",
                "Chào bạn,\n\nVui lòng click link để xác minh:\n" + link + "\n\nLink hết hạn sau 24h.");
    }

    public void sendResetPassword(String to, String token) {
        String link = baseUrl + "/api/v1/auth/reset-password?token=" + token;
        sendSimple(to, "Đặt lại mật khẩu",
                "Chào bạn,\n\nVui lòng click link để đặt lại mật khẩu:\n" + link + "\n\nLink hết hạn sau 1h.");
    }

    private void sendSimple(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mail.send(msg);
        } catch (Exception e) {
            // Không fail transaction vì email — SRS FR-IAM-004
            log.error("Send email failed to={} subject={}", to, subject, e);
        }
    }
}