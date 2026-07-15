CREATE TABLE employees (
    id               BIGSERIAL PRIMARY KEY,
    uid              VARCHAR(100) NOT NULL UNIQUE,
    name             VARCHAR(200) NOT NULL,
    role             VARCHAR(50)  NOT NULL,
    password_hash    TEXT         NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    force_password_change BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_employees_uid ON employees(uid);
