CREATE TABLE prescription_items
(
    id              UUID PRIMARY KEY,
    prescription_id UUID         NOT NULL,
    product         VARCHAR(150) NOT NULL,
    quantity        INTEGER      NOT NULL,
    batch           VARCHAR(50)  NOT NULL,
    expiry          VARCHAR(7)   NOT NULL,
    observations    TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT fk_prescription_items_prescription
        FOREIGN KEY (prescription_id) REFERENCES prescriptions (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_prescription_items_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT chk_prescription_items_expiry_format
        CHECK (expiry ~ '^(0[1-9]|1[0-2])/[0-9]{4}$')
);

CREATE INDEX idx_prescription_items_prescription_id
    ON prescription_items (prescription_id) WHERE deleted_at IS NULL;
