CREATE TABLE users
(
    id UUID PRIMARY KEY,
    name     VARCHAR(100) NOT NULL UNIQUE,
    pin_hash VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL,
    active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_users_role CHECK (role IN ('SELLER', 'ADMIN'))
);

CREATE INDEX idx_users_active ON users (active) WHERE deleted_at IS NULL;