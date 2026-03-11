-- V1__initial_schema.sql
-- Mizan EGX Portfolio Tracker — Database Schema

-- ═══════════════════════════════════════════
-- Extensions
-- ═══════════════════════════════════════════
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ═══════════════════════════════════════════
-- ENUM types
-- ═══════════════════════════════════════════
CREATE TYPE auth_provider AS ENUM ('GOOGLE', 'APPLE');
CREATE TYPE audit_action AS ENUM (
    'SIGN_UP', 'SIGN_IN', 'SIGN_OUT', 'TOKEN_REFRESH',
    'BIOMETRIC_ENABLED', 'BIOMETRIC_DISABLED',
    'ORDER_CREATED', 'ORDER_DELETED',
    'PROFILE_UPDATED', 'API_KEY_UPDATED',
    'AUTH_FAILED', 'RATE_LIMITED'
);

-- ═══════════════════════════════════════════
-- Users
-- ═══════════════════════════════════════════
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    auth_provider   auth_provider NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    profile_image_url VARCHAR(512),
    biometric_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    twelve_data_api_key_encrypted VARCHAR(512),  -- AES-256 encrypted
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMP WITH TIME ZONE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_users_provider UNIQUE (auth_provider, provider_user_id),
    CONSTRAINT uk_users_email_provider UNIQUE (email, auth_provider)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(is_active) WHERE is_active = TRUE;

-- ═══════════════════════════════════════════
-- Linked accounts (for account linking)
-- ═══════════════════════════════════════════
CREATE TABLE linked_accounts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    auth_provider   auth_provider NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email  VARCHAR(255),
    linked_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_linked_provider UNIQUE (auth_provider, provider_user_id)
);

CREATE INDEX idx_linked_user ON linked_accounts(user_id);

-- ═══════════════════════════════════════════
-- Sessions (refresh tokens)
-- ═══════════════════════════════════════════
CREATE TABLE sessions (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token_hash  VARCHAR(128) NOT NULL,   -- SHA-256 of refresh token
    device_id           VARCHAR(255),
    device_info         VARCHAR(512),            -- user-agent or device model
    ip_address          VARCHAR(45),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at          TIMESTAMP WITH TIME ZONE,  -- null = active

    CONSTRAINT uk_sessions_token UNIQUE (refresh_token_hash)
);

CREATE INDEX idx_sessions_user ON sessions(user_id);
CREATE INDEX idx_sessions_active ON sessions(user_id, revoked_at) WHERE revoked_at IS NULL;
CREATE INDEX idx_sessions_expires ON sessions(expires_at);

-- ═══════════════════════════════════════════
-- Portfolios
-- ═══════════════════════════════════════════
CREATE TABLE portfolios (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL DEFAULT 'My Portfolio',
    description VARCHAR(512),
    is_default  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_portfolio_default UNIQUE (user_id, is_default) -- only one default
);

CREATE INDEX idx_portfolios_user ON portfolios(user_id);

-- ═══════════════════════════════════════════
-- Buy Orders
-- ═══════════════════════════════════════════
CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    portfolio_id    UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ticker          VARCHAR(20) NOT NULL,
    stock_name      VARCHAR(255),
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    price_per_share DECIMAL(12,4) NOT NULL CHECK (price_per_share > 0),
    commission      DECIMAL(10,4) DEFAULT 0,
    total_cost      DECIMAL(14,4) GENERATED ALWAYS AS (quantity * price_per_share + COALESCE(commission, 0)) STORED,
    buy_date        DATE NOT NULL,
    notes           VARCHAR(512),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_orders_portfolio ON orders(portfolio_id);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_ticker ON orders(portfolio_id, ticker);
CREATE INDEX idx_orders_date ON orders(buy_date);

-- ═══════════════════════════════════════════
-- Audit Log
-- ═══════════════════════════════════════════
CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    action      audit_action NOT NULL,
    ip_address  VARCHAR(45),
    device_info VARCHAR(512),
    details     JSONB,  -- flexible metadata
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

-- Partition audit_logs by month for performance (optional, uncomment for production)
-- CREATE TABLE audit_logs (...) PARTITION BY RANGE (created_at);

-- ═══════════════════════════════════════════
-- Updated_at trigger
-- ═══════════════════════════════════════════
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_portfolios_updated_at BEFORE UPDATE ON portfolios
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
