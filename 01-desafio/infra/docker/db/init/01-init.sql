CREATE TABLE IF NOT EXISTS persons (
                                     id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    birth_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NULL,

    CONSTRAINT persons_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT persons_email_not_blank CHECK (btrim(email) <> '')
    );

CREATE INDEX IF NOT EXISTS idx_persons_created_at ON persons (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_persons_birth_date ON persons (birth_date);