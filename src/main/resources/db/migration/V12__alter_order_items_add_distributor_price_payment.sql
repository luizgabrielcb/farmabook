UPDATE order_items SET quantity = 1 WHERE quantity IS NULL;
ALTER TABLE order_items ALTER COLUMN quantity SET NOT NULL;

ALTER TABLE order_items
    ADD COLUMN distributor_id   UUID REFERENCES distributors (id) ON DELETE RESTRICT,
    ADD COLUMN distributor_name VARCHAR(100),
    ADD COLUMN price            DECIMAL(8, 2),
    ADD COLUMN payment_status   VARCHAR(20) NOT NULL DEFAULT 'TO_PAY';

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_price_positive   CHECK (price IS NULL OR price >= 0),
    ADD CONSTRAINT chk_order_items_payment_status   CHECK (payment_status IN ('TO_PAY', 'MAKE_NOTE', 'PAID', 'NOTED'));
