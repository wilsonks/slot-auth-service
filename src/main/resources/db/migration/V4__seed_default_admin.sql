-- Default bootstrap admin: uid=admin, ****** (BCrypt hash, cost=12).
-- The plaintext password is communicated to operators out-of-band and must be
-- rotated immediately after first login. force_password_change=TRUE enforces this.
INSERT INTO employees (uid, name, role, password_hash, active, force_password_change)
VALUES (
    'admin',
    'Default Administrator',
    'ADMIN',
    '$2b$12$D.XHKj1KgCQoVeQi/1.m/uZFc6O3/DTQkAZSwetjDaLm2nmIc27Zm',
    TRUE,
    TRUE
)
ON CONFLICT (uid) DO NOTHING;
