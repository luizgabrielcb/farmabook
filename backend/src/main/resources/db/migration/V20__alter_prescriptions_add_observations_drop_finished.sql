ALTER TABLE prescriptions
    DROP CONSTRAINT chk_prescriptions_finished_consistency,
    DROP COLUMN finished_by_id,
    DROP COLUMN finished_by_name,
    DROP COLUMN finished_at,
    ADD COLUMN observations TEXT;
