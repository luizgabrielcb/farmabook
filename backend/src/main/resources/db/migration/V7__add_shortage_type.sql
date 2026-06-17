ALTER TABLE shortages
    ADD COLUMN shortage_type VARCHAR(20) NOT NULL DEFAULT 'WANIA';

ALTER TABLE shortages
    ADD CONSTRAINT chk_shortages_type
        CHECK (shortage_type IN ('WANIA', 'FRANCISCO'));

ALTER TABLE shortages
    ALTER COLUMN shortage_type DROP DEFAULT;
