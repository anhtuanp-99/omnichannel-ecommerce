package com.omni.notification.email;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmailSender {

    private final JavaMailSender mail;
    private final String from;

    public EmailSender(JavaMailSender mail,
                       @Value("${app.mail.from:noreply@omni-ecom.local}") String from) {
        this.mail = mail;
        this.from = from;
    }

    public String sendHtml(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mail.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mail.send(message);
        return message.getMessageID() != null ? message.getMessageID() : "mailpit-" + UUID.randomUUID();
    }
}