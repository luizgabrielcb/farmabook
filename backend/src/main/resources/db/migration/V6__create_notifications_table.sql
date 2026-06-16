CREATE TABLE notifications
(
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    customer_phone VARCHAR(20)  NOT NULL,
    customer_name  VARCHAR(150) NOT NULL,
    message        TEXT         NOT NULL,
    link           TEXT         NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_notifications_order_id ON notifications (order_id);