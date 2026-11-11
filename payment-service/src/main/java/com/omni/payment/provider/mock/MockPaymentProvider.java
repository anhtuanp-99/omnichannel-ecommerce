package com.omni.payment.provider.mock;

import com.omni.payment.common.PaymentConstants;
import com.omni.payment.provider.PaymentInitResult;
import com.omni.payment.provider.PaymentProvider;
import com.omni.payment.provider.RefundResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentProvider implements PaymentProvider {

    private final MockProviderProperties props;

    public MockPaymentProvider(MockProviderProperties props) {
        this.props = props;
    }

    @Override
    public String name() {
        return PaymentConstants.PROVIDER_MOCK;
    }

    @Override
    public PaymentInitResult createPayment(String paymentId, long amount, String currency) {
        String providerRef = "mock-" + UUID.randomUUID();
        String url = props.getBaseUrl() + "/api/v1/mock-provider/checkout"
                + "?paymentId=" + paymentId
                + "&providerRef=" + providerRef
                + "&amount=" + amount
                + "&currency=" + currency;
        return new PaymentInitResult(providerRef, url);
    }

    @Override
    public RefundResult refund(String providerRef, long amount, String reason) {
        return new RefundResult(true, "mock-refund-" + UUID.randomUUID(), null);
    }
}