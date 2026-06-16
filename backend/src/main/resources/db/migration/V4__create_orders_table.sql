CREATE TABLE orders
(
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    customer_name   VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    notified_at TIMESTAMPTZ,
    created_by_id UUID NOT NULL,
    created_by_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_orders_status
        CHECK (status IN ('PENDING', 'ORDERED', 'RECEIVED', 'DELIVERED'))
);

CREATE INDEX idx_orders_customer_id
    ON orders (customer_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_orders_status
    ON orders (status) WHERE deleted_at IS NULL;