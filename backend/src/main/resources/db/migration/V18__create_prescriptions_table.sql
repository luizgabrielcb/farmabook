CREATE TABLE prescriptions
(
    id               UUID PRIMARY KEY,
    customer_id      UUID         NOT NULL,
    customer_name    VARCHAR(100) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_by_id    UUID         NOT NULL,
    created_by_name  VARCHAR(100) NOT NULL,
    finished_by_id   UUID,
    finished_by_name VARCHAR(100),
    finished_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    deleted_at       TIMESTAMPTZ,

    CONSTRAINT fk_prescriptions_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_prescriptions_status
        CHECK (status IN ('PENDING', 'FINISHED')),

    CONSTRAINT chk_prescriptions_finished_consistency
        CHECK (
            (status = 'PENDING'
                AND finished_by_id IS NULL
                AND finished_by_name IS NULL
                AND finished_at IS NULL)
            OR (status = 'FINISHED'
                AND finished_by_id IS NOT NULL
                AND finished_by_name IS NOT NULL
                AND finished_at IS NOT NULL)
            )
);

CREATE INDEX idx_prescriptions_customer_id
    ON prescriptions (customer_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_prescriptions_status
    ON prescriptions (status) WHERE deleted_at IS NULL;
