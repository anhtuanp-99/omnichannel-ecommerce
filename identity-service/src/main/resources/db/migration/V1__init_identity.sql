CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(255) NOT NULL,
                       phone VARCHAR(20),
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(100) NOT NULL,
                       status VARCHAR(20) NOT NULL,           -- PENDING_VERIFY, ACTIVE, LOCKED, DISABLED
                       email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                       failed_login_count INT NOT NULL DEFAULT 0,
                       locked_until TIMESTAMPTZ,
                       created_at TIMESTAMPTZ NOT NULL,
                       updated_at TIMESTAMPTZ NOT NULL,
                       deleted_at TIMESTAMPTZ,
                       CONSTRAINT uq_users_email UNIQUE (email)
);
CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;

CREATE TABLE roles (
                       id UUID PRIMARY KEY,
                       code VARCHAR(50) NOT NULL,
                       name VARCHAR(100) NOT NULL,
                       CONSTRAINT uq_roles_code UNIQUE (code)
);

CREATE TABLE user_roles (
                            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                            PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY,
                                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                token_hash VARCHAR(64) NOT NULL,           -- SHA-256 hex
                                expires_at TIMESTAMPTZ NOT NULL,
                                revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                replaced_by UUID,                          -- rotation chain
                                user_agent VARCHAR(255),
                                ip_address VARCHAR(45),
                                created_at TIMESTAMPTZ NOT NULL,
                                CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);

CREATE TABLE verification_tokens (
                                     id UUID PRIMARY KEY,
                                     user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                     token_hash VARCHAR(64) NOT NULL,
                                     purpose VARCHAR(30) NOT NULL,              -- VERIFY_EMAIL, RESET_PASSWORD
                                     expires_at TIMESTAMPTZ NOT NULL,
                                     used BOOLEAN NOT NULL DEFAULT FALSE,
                                     created_at TIMESTAMPTZ NOT NULL,
                                     CONSTRAINT uq_verification_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_verification_user_purpose ON verification_tokens(user_id, purpose);

CREATE TABLE addresses (
                           id UUID PRIMARY KEY,
                           user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           recipient VARCHAR(100) NOT NULL,
                           phone VARCHAR(20) NOT NULL,
                           province VARCHAR(100) NOT NULL,
                           district VARCHAR(100) NOT NULL,
                           ward VARCHAR(100) NOT NULL,
                           detail VARCHAR(255) NOT NULL,
                           is_default BOOLEAN NOT NULL DEFAULT FALSE,
                           created_at TIMESTAMPTZ NOT NULL,
                           updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_addresses_user ON addresses(user_id);
-- Chỉ 1 default per user (partial unique index)
CREATE UNIQUE INDEX uq_address_default_per_user
    ON addresses(user_id) WHERE is_default = TRUE;

CREATE TABLE audit_logs (
                            id UUID PRIMARY KEY,
                            actor_id UUID,
                            action VARCHAR(100) NOT NULL,
                            target_type VARCHAR(50),
                            target_id VARCHAR(255),
                            delta JSONB,
                            ip_address VARCHAR(45),
                            user_agent VARCHAR(255),
                            correlation_id VARCHAR(64),
                            result VARCHAR(20) NOT NULL,               -- SUCCESS, FAILURE
                            occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_audit_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_action_time ON audit_logs(action, occurred_at DESC);