package com.omni.payment.provider;

public record RefundResult(boolean success, String providerRef, String failureReason) {}