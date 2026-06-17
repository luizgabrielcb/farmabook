CREATE TABLE shortage_orders
(
    id               UUID         PRIMARY KEY,
    shortage_type    VARCHAR(20)  NOT NULL,
    distributor_id   UUID         NOT NULL REFERENCES distributors (id),
    distributor_name VARCHAR(100) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    created_by_id    UUID         NOT NULL,
    created_by_name  VARCHAR(100) NOT NULL,

    ordered_by_id    UUID,
    ordered_by_name  VARCHAR(100),
    ordered_at       TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT chk_shortage_orders_status
        CHECK (status IN ('PENDING', 'ORDERED')),

    CONSTRAINT chk_shortage_orders_type
        CHECK (shortage_type IN ('WANIA', 'FRANCISCO'))
);

CREATE INDEX idx_shortage_orders_shortage_type ON shortage_orders (shortage_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_shortage_orders_distributor_id ON shortage_orders (distributor_id) WHERE deleted_at IS NULL;
