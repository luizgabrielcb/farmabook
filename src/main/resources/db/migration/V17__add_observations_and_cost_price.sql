ALTER TABLE shortage_orders
    ADD COLUMN observations TEXT;

ALTER TABLE shortages
    ADD COLUMN cost_price NUMERIC(8, 2);
