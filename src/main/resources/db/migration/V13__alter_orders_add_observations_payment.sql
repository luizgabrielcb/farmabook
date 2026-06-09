ALTER TABLE orders
    ADD COLUMN observations  TEXT,
    ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'TO_PAY';

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_payment_status CHECK (payment_status IN ('TO_PAY', 'MAKE_NOTE', 'PAID', 'NOTED'));
