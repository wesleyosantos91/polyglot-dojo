CREATE TABLE persons (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    birth_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT persons_name_not_blank CHECK (TRIM(name) <> ''),
    CONSTRAINT persons_email_not_blank CHECK (TRIM(email) <> '')
);

CREATE INDEX idx_persons_created_at ON persons (created_at DESC);
CREATE INDEX idx_persons_birth_date ON persons (birth_date);
