CREATE TABLE distributors
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_distributors_name ON distributors (name) WHERE deleted_at IS NULL;
