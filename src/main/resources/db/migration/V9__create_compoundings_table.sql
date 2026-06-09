CREATE TABLE compoundings
(
    id                UUID         PRIMARY KEY,
    quantity          INTEGER      NOT NULL,
    customer_id       UUID         NOT NULL REFERENCES customers (id) ON DELETE RESTRICT,
    customer_name     VARCHAR(100) NOT NULL,
    pharmacy_id       UUID         NOT NULL REFERENCES compounding_pharmacies (id) ON DELETE RESTRICT,
    pharmacy_name     VARCHAR(150) NOT NULL,
    pharmacy_city     VARCHAR(100) NOT NULL,
    value             NUMERIC(10, 2),
    observations      TEXT,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    payment_status    VARCHAR(20)  NOT NULL DEFAULT 'TO_PAY',
    notified_at       TIMESTAMPTZ,
    created_by_id     UUID         NOT NULL,
    created_by_name   VARCHAR(100) NOT NULL,
    ordered_by_id     UUID,
    ordered_by_name   VARCHAR(100),
    ordered_at        TIMESTAMPTZ,
    received_by_id    UUID,
    received_by_name  VARCHAR(100),
    received_at       TIMESTAMPTZ,
    delivered_by_id   UUID,
    delivered_by_name VARCHAR(100),
    delivered_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    deleted_at        TIMESTAMPTZ,

    CONSTRAINT chk_compoundings_status
        CHECK (status IN ('PENDING', 'ORDERED', 'RECEIVED', 'DELIVERED')),

    CONSTRAINT chk_compoundings_payment_status
        CHECK (payment_status IN ('TO_PAY', 'PAID', 'MAKE_NOTE', 'NOTED'))
);

CREATE INDEX idx_compoundings_customer_id ON compoundings (customer_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_compoundings_status ON compoundings (status) WHERE deleted_at IS NULL;
CREATE INDEX idx_compoundings_payment_status ON compoundings (payment_status) WHERE deleted_at IS NULL;
