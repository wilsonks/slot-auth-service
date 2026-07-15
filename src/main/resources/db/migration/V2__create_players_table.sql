CREATE TABLE players (
    id         BIGSERIAL PRIMARY KEY,
    uid        VARCHAR(100) NOT NULL UNIQUE,
    nickname   VARCHAR(100),
    pin_hash   TEXT,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_players_uid ON players(uid);
