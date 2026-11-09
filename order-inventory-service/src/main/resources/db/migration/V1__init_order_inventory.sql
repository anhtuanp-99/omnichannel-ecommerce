-- =====================================================
-- CART
-- =====================================================
CREATE TABLE carts (
                       id UUID PRIMARY KEY,
                       customer_id UUID NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL,
                       updated_at TIMESTAMPTZ NOT NULL,
                       CONSTRAINT uq_carts_customer UNIQUE (customer_id)
);

CREATE TABLE cart_items (
                            id UUID PRIMARY KEY,
                            cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
                            sku_id UUID NOT NULL,
                            quantity INT NOT NULL CHECK (quantity > 0),
                            added_at TIMESTAMPTZ NOT NULL,
                            CONSTRAINT uq_cart_items_cart_sku UNIQUE (cart_id, sku_id)
);
CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);

-- =====================================================
-- ORDER
-- =====================================================
CREATE TABLE orders (
                        id UUID PRIMARY KEY,
                        code VARCHAR(50) NOT NULL,
                        customer_id UUID NOT NULL,
                        status VARCHAR(20) NOT NULL,                -- PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED
                        subtotal BIGINT NOT NULL,
                        discount BIGINT NOT NULL DEFAULT 0,
                        tax BIGINT NOT NULL DEFAULT 0,
                        shipping_fee BIGINT NOT NULL DEFAULT 0,
                        total_amount BIGINT NOT NULL,
                        currency VARCHAR(3) NOT NULL,
                        shipping_address_snapshot JSONB NOT NULL,
                        contact_snapshot JSONB NOT NULL,
                        cancel_reason VARCHAR(500),
                        created_at TIMESTAMPTZ NOT NULL,
                        updated_at TIMESTAMPTZ NOT NULL,
                        CONSTRAINT uq_orders_code UNIQUE (code)
);
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created ON orders(created_at DESC);

CREATE TABLE order_items (
                             id UUID PRIMARY KEY,
                             order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             sku_id UUID NOT NULL,
                             sku_code VARCHAR(100) NOT NULL,
                             product_name VARCHAR(255) NOT NULL,
                             attributes_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
                             unit_price BIGINT NOT NULL,
                             quantity INT NOT NULL CHECK (quantity > 0),
                             line_total BIGINT NOT NULL
);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_sku ON order_items(sku_id);

CREATE TABLE order_status_history (
                                      id UUID PRIMARY KEY,
                                      order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                                      old_status VARCHAR(20),
                                      new_status VARCHAR(20) NOT NULL,
                                      reason VARCHAR(500),
                                      actor VARCHAR(100),
                                      occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_order_status_history_order ON order_status_history(order_id, occurred_at);

-- =====================================================
-- INVENTORY
-- =====================================================
-- Warehouse là constant UUID trong app config (v1.0 một kho).
-- available = on_hand - reserved (không lưu cột riêng để tránh inconsistency).
CREATE TABLE stocks (
                        id UUID PRIMARY KEY,
                        sku_id UUID NOT NULL,
                        warehouse_id UUID NOT NULL,
                        on_hand BIGINT NOT NULL CHECK (on_hand >= 0),
                        reserved BIGINT NOT NULL DEFAULT 0 CHECK (reserved >= 0),
                        version BIGINT NOT NULL DEFAULT 0,
                        updated_at TIMESTAMPTZ NOT NULL,
                        CONSTRAINT uq_stocks_sku_wh UNIQUE (sku_id, warehouse_id),
    -- available luôn >= 0 — enforce ở application, DB constraint dưới đây là defense-in-depth
                        CONSTRAINT chk_stocks_available_nonneg CHECK (on_hand - reserved >= 0)
);
CREATE INDEX idx_stocks_sku ON stocks(sku_id);

CREATE TABLE stock_reservations (
                                    id UUID PRIMARY KEY,
                                    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                                    sku_id UUID NOT NULL,
                                    warehouse_id UUID NOT NULL,
                                    quantity INT NOT NULL CHECK (quantity > 0),
                                    status VARCHAR(20) NOT NULL,                -- PENDING, COMMITTED, RELEASED, EXPIRED
                                    expires_at TIMESTAMPTZ NOT NULL,
                                    created_at TIMESTAMPTZ NOT NULL,
                                    updated_at TIMESTAMPTZ NOT NULL,
                                    version BIGINT NOT NULL DEFAULT 0           -- <--- THÊM DÒNG NÀY VÀO ĐÂY
);
CREATE INDEX idx_reservations_order ON stock_reservations(order_id);
CREATE INDEX idx_reservations_expiry ON stock_reservations(status, expires_at)
    WHERE status = 'PENDING';

CREATE TABLE stock_movements (
                                 id UUID PRIMARY KEY,
                                 sku_id UUID NOT NULL,
                                 warehouse_id UUID NOT NULL,
                                 quantity BIGINT NOT NULL,                   -- dương = nhập/commit; âm = release (theo chiều nghiệp vụ)
                                 type VARCHAR(20) NOT NULL,                  -- IMPORT, ADJUST, RESERVE, RELEASE, COMMIT
                                 reference_type VARCHAR(50),
                                 reference_id VARCHAR(100),
                                 actor VARCHAR(100),
                                 occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_movements_sku ON stock_movements(sku_id, occurred_at DESC);
CREATE INDEX idx_movements_reference ON stock_movements(reference_type, reference_id);

-- =====================================================
-- IDEMPOTENCY
-- =====================================================
CREATE TABLE idempotency_keys (
                                  key VARCHAR(255) PRIMARY KEY,
                                  request_hash VARCHAR(64) NOT NULL,
                                  response_json JSONB,
                                  status VARCHAR(20) NOT NULL,                -- PROCESSING, COMPLETED
                                  created_at TIMESTAMPTZ NOT NULL,
                                  expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);

-- =====================================================
-- OUTBOX
-- =====================================================
CREATE TABLE outbox_events (
                               event_id UUID PRIMARY KEY,
                               aggregate_type VARCHAR(50) NOT NULL,
                               aggregate_id UUID NOT NULL,
                               event_type VARCHAR(100) NOT NULL,
                               event_version VARCHAR(10) NOT NULL DEFAULT '1',
                               payload JSONB NOT NULL,
                               status VARCHAR(20) NOT NULL,                -- PENDING, PUBLISHED, FAILED
                               retry_count INT NOT NULL DEFAULT 0,
                               created_at TIMESTAMPTZ NOT NULL,
                               published_at TIMESTAMPTZ
);
CREATE INDEX idx_outbox_pending ON outbox_events(created_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);

-- =====================================================
-- PROCESSED EVENTS (consumer idempotent)
-- =====================================================
CREATE TABLE processed_events (
                                  event_id UUID PRIMARY KEY,
                                  event_type VARCHAR(100) NOT NULL,
                                  processed_at TIMESTAMPTZ NOT NULL
);