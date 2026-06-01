CREATE TABLE order_items
(
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,

    product           VARCHAR(150) NOT NULL,
    category          VARCHAR(20)  NOT NULL,
    quantity          INTEGER,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    ordered_by_id UUID,
    ordered_by_name   VARCHAR(100),
    ordered_at TIMESTAMPTZ,

    received_by_id UUID,
    received_by_name  VARCHAR(100),
    received_at TIMESTAMPTZ,

    delivered_by_id UUID,
    delivered_by_name VARCHAR(100),
    delivered_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_order_items_status
        CHECK (status IN ('PENDING', 'ORDERED', 'RECEIVED', 'DELIVERED')),

    CONSTRAINT chk_order_items_category
        CHECK (category IN ('ETICO', 'GENERICO', 'SIMILAR', 'PERFUMARIA',
                            'SUPLEMENTO', 'LIBERADO', 'OUTROS')),

    CONSTRAINT chk_order_items_quantity_positive
        CHECK (quantity IS NULL OR quantity > 0),

    CONSTRAINT chk_order_items_ordered_consistency
        CHECK (
            (status = 'PENDING'
                AND ordered_by_id IS NULL
                AND ordered_by_name IS NULL
                AND ordered_at IS NULL)
                OR (status IN ('ORDERED', 'RECEIVED', 'DELIVERED')
                AND ordered_by_id IS NOT NULL
                AND ordered_by_name IS NOT NULL
                AND ordered_at IS NOT NULL)
            ),

    CONSTRAINT chk_order_items_received_consistency
        CHECK (
            (status IN ('PENDING', 'ORDERED')
                AND received_by_id IS NULL
                AND received_by_name IS NULL
                AND received_at IS NULL)
                OR (status IN ('RECEIVED', 'DELIVERED')
                AND received_by_id IS NOT NULL
                AND received_by_name IS NOT NULL
                AND received_at IS NOT NULL)
            ),

    CONSTRAINT chk_order_items_delivered_consistency
        CHECK (
            (status IN ('PENDING', 'ORDERED', 'RECEIVED')
                AND delivered_by_id IS NULL
                AND delivered_by_name IS NULL
                AND delivered_at IS NULL)
                OR (status = 'DELIVERED'
                AND delivered_by_id IS NOT NULL
                AND delivered_by_name IS NOT NULL
                AND delivered_at IS NOT NULL)
            )
);

CREATE INDEX idx_order_items_order_id
    ON order_items (order_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_order_items_order_status
    ON order_items (order_id, status) WHERE deleted_at IS NULL;