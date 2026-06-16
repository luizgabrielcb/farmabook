ALTER TABLE order_items
    ADD COLUMN payment_changed_by_id   UUID,
    ADD COLUMN payment_changed_by_name VARCHAR(100),
    ADD COLUMN payment_changed_at      TIMESTAMPTZ;

ALTER TABLE compoundings
    ADD COLUMN payment_changed_by_id   UUID,
    ADD COLUMN payment_changed_by_name VARCHAR(100),
    ADD COLUMN payment_changed_at      TIMESTAMPTZ;

ALTER TABLE prescriptions
    ALTER COLUMN customer_id DROP NOT NULL,
    ALTER COLUMN customer_name DROP NOT NULL;
