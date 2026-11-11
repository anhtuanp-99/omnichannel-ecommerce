package com.omni.payment.provider.mock;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.provider.mock")
public class MockProviderProperties {
    private String hmacSecret;
    private String baseUrl = "http://localhost:8084";
    private String webhookUrl = "http://localhost:8084/api/v1/payments/webhook";

    public String getHmacSecret() { return hmacSecret; }
    public void setHmacSecret(String hmacSecret) { this.hmacSecret = hmacSecret; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
}