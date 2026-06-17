ALTER TABLE notifications ALTER COLUMN order_id DROP NOT NULL;
ALTER TABLE notifications ADD COLUMN compounding_id UUID REFERENCES compoundings(id) ON DELETE RESTRICT;

ALTER TABLE notifications ADD CONSTRAINT chk_notifications_source
    CHECK (
        (order_id IS NOT NULL AND compounding_id IS NULL) OR
        (order_id IS NULL AND compounding_id IS NOT NULL)
    );

CREATE INDEX idx_notifications_compounding_id ON notifications (compounding_id) WHERE deleted_at IS NULL;
