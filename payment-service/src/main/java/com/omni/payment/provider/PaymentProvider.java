package com.omni.payment.provider;

public interface PaymentProvider {
    String name();
    PaymentInitResult createPayment(String paymentId, long amount, String currency);
    RefundResult refund(String providerRef, long amount, String reason);
}