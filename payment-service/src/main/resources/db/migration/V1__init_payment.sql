CREATE TABLE payments (
                          id UUID PRIMARY KEY,
                          order_id UUID NOT NULL,
                          amount BIGINT NOT NULL CHECK (amount > 0),
                          refunded_amount BIGINT NOT NULL DEFAULT 0 CHECK (refunded_amount >= 0),
                          currency VARCHAR(3) NOT NULL,
                          provider VARCHAR(20) NOT NULL,
                          provider_ref VARCHAR(255),
                          status VARCHAR(20) NOT NULL,
                          failure_category VARCHAR(50),
                          created_at TIMESTAMPTZ NOT NULL,
                          updated_at TIMESTAMPTZ NOT NULL,
                          CONSTRAINT uq_payments_order UNIQUE (order_id),
                          CONSTRAINT chk_payments_refund_le_amount CHECK (refunded_amount <= amount)
);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_order ON payments(order_id);

CREATE TABLE payment_refunds (
                                 id UUID PRIMARY KEY,
                                 payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
                                 amount BIGINT NOT NULL CHECK (amount > 0),
                                 reason VARCHAR(500) NOT NULL,
                                 status VARCHAR(20) NOT NULL,
                                 provider_ref VARCHAR(255),
                                 requested_by UUID NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL,
                                 completed_at TIMESTAMPTZ
);
CREATE INDEX idx_refunds_payment ON payment_refunds(payment_id);

-- Lưu webhook đã xử lý — chống replay + audit
CREATE TABLE webhook_events (
                                id UUID PRIMARY KEY,
                                provider VARCHAR(20) NOT NULL,
                                provider_txn_id VARCHAR(255) NOT NULL,
                                event_id VARCHAR(255) NOT NULL,
                                payment_id UUID,
                                raw_payload JSONB NOT NULL,
                                signature_valid BOOLEAN NOT NULL,
                                processed_at TIMESTAMPTZ NOT NULL,
                                CONSTRAINT uq_webhook_provider_txn UNIQUE (provider, provider_txn_id)
);
CREATE INDEX idx_webhook_payment ON webhook_events(payment_id);

CREATE TABLE outbox_events (
                               event_id UUID PRIMARY KEY,
                               aggregate_type VARCHAR(50) NOT NULL,
                               aggregate_id UUID NOT NULL,
                               event_type VARCHAR(100) NOT NULL,
                               event_version VARCHAR(10) NOT NULL DEFAULT '1',
                               payload JSONB NOT NULL,
                               status VARCHAR(20) NOT NULL,
                               retry_count INT NOT NULL DEFAULT 0,
                               created_at TIMESTAMPTZ NOT NULL,
                               published_at TIMESTAMPTZ
);
CREATE INDEX idx_outbox_pending ON outbox_events(created_at) WHERE status = 'PENDING';

CREATE TABLE processed_events (
                                  event_id UUID PRIMARY KEY,
                                  event_type VARCHAR(100) NOT NULL,
                                  processed_at TIMESTAMPTZ NOT NULL
);