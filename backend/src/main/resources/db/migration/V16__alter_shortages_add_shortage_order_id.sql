ALTER TABLE shortages
    ADD COLUMN shortage_order_id UUID REFERENCES shortage_orders (id);

CREATE INDEX idx_shortages_shortage_order_id ON shortages (shortage_order_id) WHERE deleted_at IS NULL;
