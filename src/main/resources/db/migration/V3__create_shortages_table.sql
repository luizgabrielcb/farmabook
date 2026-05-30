CREATE TABLE shortages
(
    id UUID PRIMARY KEY,
    product         VARCHAR(150) NOT NULL,
    category        VARCHAR(20)  NOT NULL,
    quantity        INTEGER,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    created_by_id UUID NOT NULL,
    created_by_name VARCHAR(100) NOT NULL,

    ordered_by_id UUID,
    ordered_by_name VARCHAR(100),
    ordered_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT chk_shortages_status
        CHECK (status IN ('PENDING', 'ORDERED')),

    CONSTRAINT chk_shortages_category
        CHECK (category IN ('ETICO', 'GENERICO', 'SIMILAR', 'PERFUMARIA',
                            'SUPLEMENTO', 'LIBERADO', 'OUTROS')),

    CONSTRAINT chk_shortages_quantity_positive
        CHECK (quantity IS NULL OR quantity > 0),

    CONSTRAINT chk_shortages_ordered_consistency
        CHECK (
            (status = 'PENDING' AND ordered_by_id IS NULL AND ordered_by_name IS NULL AND ordered_at IS NULL)
                OR (status = 'ORDERED' AND ordered_by_id IS NOT NULL AND ordered_by_name IS NOT NULL AND
                    ordered_at IS NOT NULL)
            )
);

CREATE INDEX idx_shortages_status ON shortages (status) WHERE deleted_at IS NULL;