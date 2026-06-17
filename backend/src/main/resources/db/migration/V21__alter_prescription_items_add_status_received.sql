ALTER TABLE prescription_items
    DROP COLUMN observations,
    ADD COLUMN status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN received_by_id   UUID,
    ADD COLUMN received_by_name VARCHAR(100),
    ADD COLUMN received_at      TIMESTAMPTZ;

ALTER TABLE prescription_items
    ADD CONSTRAINT chk_prescription_items_status
        CHECK (status IN ('PENDING', 'RECEIVED')),
    ADD CONSTRAINT chk_prescription_items_received_consistency
        CHECK (
            (status = 'PENDING'
                AND received_by_id IS NULL
                AND received_by_name IS NULL
                AND received_at IS NULL)
            OR (status = 'RECEIVED'
                AND received_by_id IS NOT NULL
                AND received_by_name IS NOT NULL
                AND received_at IS NOT NULL)
            );

CREATE INDEX idx_prescription_items_status
    ON prescription_items (prescription_id, status) WHERE deleted_at IS NULL;
