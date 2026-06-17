ALTER TABLE orders ADD COLUMN total_price DECIMAL(8, 2);
ALTER TABLE orders ADD CONSTRAINT chk_orders_total_price_positive CHECK (total_price IS NULL OR total_price >= 0);
