CREATE TABLE revoked_tokens (
    id         BIGSERIAL PRIMARY KEY,
    jti        VARCHAR(100) NOT NULL UNIQUE,
    subject    VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_revoked_tokens_jti ON revoked_tokens(jti);
CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens(expires_at);
