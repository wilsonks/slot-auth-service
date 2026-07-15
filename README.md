# slot-auth-service

**Identity and Role Management** microservice for the `slot-central` EGM (Electronic Gaming Machine) slot-floor platform. Owns a dedicated PostgreSQL database and is the single source of truth for authentication and authorization of both **players** (card/UID-based EGM sessions) and **staff/employees**.

> Part of a re-architecture from a monolithic `slot-central-server-express-rmq` Node.js backend into Spring Boot microservices.

---

## Table of Contents

1. [Service Role](#service-role)
2. [Sibling Services](#sibling-services)
3. [Tech Stack](#tech-stack)
4. [API Endpoints](#api-endpoints)
5. [JWT Claim Shape](#jwt-claim-shape)
6. [JWKS & Key Management](#jwks--key-management)
7. [Environment Variables](#environment-variables)
8. [Running Locally](#running-locally)
9. [Running Tests](#running-tests)
10. [Docker Compose](#docker-compose)
11. [Database Migrations](#database-migrations)
12. [Revocation Mechanism](#revocation-mechanism)
13. [Security TODOs](#security-todos)

---

## Service Role

`slot-auth-service` is responsible for:

- **Employee authentication** — BCrypt-verified password login producing a signed RS256 JWT with a role claim (`ADMIN`, `FLOOR_MANAGER`, `CAGE_CASHIER`, `TECHNICIAN`).
- **Player card sessions** — A card swipe/UID scan produces a short-lived JWT so the EGM can identify the session. No password required; the physical card is the credential.
- **Identity management** — CRUD for employee records (admin-only) and player profile data (nickname, PIN, hold status).
- **Token revocation** — Revoked JTIs are stored in the `revoked_tokens` table. An expiry-based cleanup query keeps the table small.
- **JWKS endpoint** — `GET /.well-known/jwks.json` exposes the public RSA key so downstream services can verify JWTs independently without calling back to this service.

---

## Sibling Services

| Service | Responsibility |
|---|---|
| `slot-floor-service` | EGM session lifecycle, credits, game events |
| `slot-egm-adapter` | Serial/IP protocol bridge to physical EGMs |
| `slot-cage-service` | Cashier operations, ticket-in/ticket-out |
| `slot-reporting-service` | Analytics, audit logs, regulatory reports |
| `slot-notification-service` | Alerts, floor-manager push notifications |

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.4 |
| Build | Gradle 8.10.2 (Groovy DSL) |
| Database | PostgreSQL 16 via Spring Data JPA |
| Migrations | Flyway |
| JWT | Nimbus JOSE JWT 9.40 (RS256) |
| Password hashing | BCrypt (Spring Security) |
| Structured logging | logstash-logback-encoder 7.4 |
| Integration tests | Testcontainers + JUnit 5 |

---

## API Endpoints

### Authentication — open (no JWT required)

#### `POST /api/v1/employees/login`
Employee password login.

**Request:**
```json
{
  "uid": "jsmith",
  "password": "SecurePass123!"
}
```

**Response `200 OK`:**
```json
{
  "token": "<RS256 JWT>",
  "uid": "jsmith",
  "role": "FLOOR_MANAGER",
  "expiresIn": 28800,
  "forcePasswordChange": false
}
```

**Errors:** `401 Unauthorized` (bad credentials or inactive account).

---

#### `POST /api/v1/players/session`
Start a player session from a card swipe or UID scan.

**Request:**
```json
{ "uid": "CARD-0042" }
```

**Response `200 OK`:**
```json
{
  "token": "<RS256 JWT>",
  "uid": "CARD-0042",
  "status": "ACTIVE",
  "expiresIn": 3600
}
```

**Errors:** `404 Not Found` (unknown card), `403 Forbidden` (card on hold).

---

### JWKS

#### `GET /.well-known/jwks.json`
Returns the RSA public key set for JWT verification by downstream services.

**Response `200 OK`:**
```json
{
  "keys": [{
    "kty": "RSA",
    "kid": "<key-id>",
    "n": "<modulus>",
    "e": "AQAB",
    "alg": "RS256",
    "use": "sig"
  }]
}
```

---

### Employee Management (requires `ADMIN` or `FLOOR_MANAGER` JWT)

| Method | Path | Role Required | Description |
|---|---|---|---|
| `POST` | `/api/v1/employees` | `ADMIN` | Create employee |
| `GET` | `/api/v1/employees` | `ADMIN`, `FLOOR_MANAGER` | List all employees |
| `GET` | `/api/v1/employees/{uid}` | `ADMIN`, `FLOOR_MANAGER` | Get employee by UID |
| `PUT` | `/api/v1/employees/{uid}` | `ADMIN` | Update name/role/active |
| `DELETE` | `/api/v1/employees/{uid}` | `ADMIN` | Delete employee |

**Create Employee Request:**
```json
{
  "uid": "jdoe",
  "name": "Jane Doe",
  "role": "CAGE_CASHIER",
  "password": "InitialPass1!"
}
```

**Employee Response:**
```json
{
  "id": 5,
  "uid": "jdoe",
  "name": "Jane Doe",
  "role": "CAGE_CASHIER",
  "active": true,
  "forcePasswordChange": false,
  "createdAt": "2024-11-01T09:00:00Z",
  "updatedAt": "2024-11-01T09:00:00Z"
}
```

---

### Player Management (requires authenticated JWT)

| Method | Path | Role Required | Description |
|---|---|---|---|
| `POST` | `/api/v1/players/{uid}/session/close` | `PLAYER`, `ADMIN`, `FLOOR_MANAGER` | Revoke player session token |
| `GET` | `/api/v1/players/{uid}` | Any authenticated | Get player profile |
| `PUT` | `/api/v1/players/{uid}/nickname` | `PLAYER`, `ADMIN`, `FLOOR_MANAGER` | Update nickname |
| `PUT` | `/api/v1/players/{uid}/pin` | `PLAYER`, `ADMIN` | Set/update PIN |
| `PUT` | `/api/v1/players/{uid}/hold` | `ADMIN`, `FLOOR_MANAGER` | Toggle hold status |

**Player Response:**
```json
{
  "id": 12,
  "uid": "CARD-0042",
  "nickname": "LuckyAce",
  "status": "ACTIVE",
  "createdAt": "2024-10-15T14:30:00Z",
  "updatedAt": "2024-11-01T09:05:00Z"
}
```

---

### Actuator

| Path | Auth | Description |
|---|---|---|
| `GET /actuator/health` | Public | Liveness/readiness |
| `GET /actuator/info` | Public | App metadata |

---

## JWT Claim Shape

### Employee token
```json
{
  "sub": "jsmith",
  "iss": "slot-auth-service",
  "iat": 1730000000,
  "exp": 1730028800,
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "type": "employee",
  "role": "FLOOR_MANAGER"
}
```

### Player session token
```json
{
  "sub": "CARD-0042",
  "iss": "slot-auth-service",
  "iat": 1730000000,
  "exp": 1730003600,
  "jti": "660f9500-f30c-52e5-b827-557766551111",
  "type": "player"
}
```

**Spring Security mapping:** The `role` claim maps to `ROLE_<VALUE>` authority (e.g., `ROLE_FLOOR_MANAGER`). Player tokens get `ROLE_PLAYER`.

---

## JWKS & Key Management

### Development / Local
When `JWT_PRIVATE_KEY_PEM` and `JWT_PUBLIC_KEY_PEM` are not set, the service **generates a new 2048-bit RSA key pair on startup**. This is convenient for local development but means tokens issued before a restart are invalidated.

### Production
Provide PKCS#8 PEM-encoded keys via environment variables:
```
JWT_PRIVATE_KEY_PEM=-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
JWT_PUBLIC_KEY_PEM=-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----
```

Generate a key pair:
```bash
openssl genrsa -out private.pem 2048
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in private.pem -out private_pkcs8.pem
openssl rsa -in private.pem -pubout -out public.pem
```

### Key Rotation (planned)
Current limitations:
- Single active key — rotation requires a rolling restart
- No `kid` selection on incoming tokens beyond matching the current key

**Planned evolution:**
1. Support a list of keys in `JWKSet` (current + previous) with TTL-based retirement
2. Add a `/admin/keys/rotate` endpoint for zero-downtime rotation
3. Consider an external key store (AWS KMS, HashiCorp Vault) for production

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | Active Spring profile (`local`, `docker`, `prod`) |
| `DB_URL` | `jdbc:postgresql://localhost:5432/slot_auth` | JDBC URL |
| `DB_USERNAME` | `slotauth` | Database user |
| `DB_PASSWORD` | `slotauth` | Database password |
| `SERVER_PORT` | `8081` | HTTP port |
| `JWT_PRIVATE_KEY_PEM` | _(empty — auto-generated)_ | PKCS#8 RSA private key PEM |
| `JWT_PUBLIC_KEY_PEM` | _(empty — auto-generated)_ | RSA public key PEM |
| `PLAYER_SESSION_TTL_SECONDS` | `3600` | Player JWT TTL (1 hour) |
| `EMPLOYEE_TOKEN_TTL_SECONDS` | `28800` | Employee JWT TTL (8 hours) |

---

## Running Locally

**Prerequisites:** Java 21, a running PostgreSQL instance.

```bash
# Create DB and user
psql -U postgres -c "CREATE DATABASE slot_auth;"
psql -U postgres -c "CREATE USER slotauth WITH PASSWORD 'slotauth';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE slot_auth TO slotauth;"

# Run
./gradlew bootRun
```

The service starts on port `8081`. Flyway migrations run automatically on startup, including the default admin seed (`uid=admin`, ****** — see [V4 migration](src/main/resources/db/migration/V4__seed_default_admin.sql) and check with your team for the initial password).

> **Security:** Change the default admin password immediately after first deployment. The `force_password_change` flag is set to `true` on the seeded admin to enforce this.

---

## Running Tests

```bash
# All tests (unit + integration — requires Docker for Testcontainers)
./gradlew test

# Unit tests only
./gradlew test --tests "com.slotcentral.auth.service.*"

# Integration tests only
./gradlew test --tests "com.slotcentral.auth.integration.*"
```

Test report: `build/reports/tests/test/index.html`

---

## Docker Compose

```bash
# Build and start everything
docker compose up --build

# Stop
docker compose down

# Stop and remove volumes
docker compose down -v
```

The stack starts PostgreSQL (with a health check) then the service. The service is available at `http://localhost:8081`.

---

## Database Migrations

Flyway migrations live in `src/main/resources/db/migration/`:

| Version | Description |
|---|---|
| V1 | `employees` table |
| V2 | `players` table |
| V3 | `revoked_tokens` table |
| V4 | Seed default admin (`uid=admin`) |

---

## Revocation Mechanism

### How it works
When a player closes a session (or an employee logs out), the token's `jti` (JWT ID) is written to the `revoked_tokens` table with its expiry timestamp. On every token validation, the service does a single `EXISTS` query against this table.

### Tradeoffs

| Aspect | Notes |
|---|---|
| **Performance** | O(1) indexed lookup on `jti` VARCHAR(100). Acceptable for EGM floor scale. |
| **Cleanup** | `deleteExpiredTokens(Instant.now())` removes expired entries — should be called on a schedule (e.g., `@Scheduled`). Not yet wired to a scheduler. |
| **Scale** | For very high token volume, consider a Redis `SET` with TTL as an alternative. |
| **Consistency** | Revocation is synchronous and durable (Postgres). No eventual-consistency lag. |
| **Coverage** | Employee tokens are not revoked on logout today — only player session tokens are. Add an `/api/v1/employees/logout` endpoint to close that gap. |

---

## Security TODOs

- [ ] **Key rotation** — Support multiple active keys with `kid` selection; `/admin/keys/rotate` endpoint
- [ ] **Refresh tokens** — Add opaque refresh tokens so employee sessions can be extended without re-login
- [ ] **Brute-force protection** — Rate-limit `/employees/login` (e.g., per-IP or per-UID lockout after N failures)
- [ ] **Employee logout** — Wire `POST /api/v1/employees/logout` to revoke the employee's token JTI
- [ ] **MFA for employees** — TOTP second factor for `ADMIN` and `FLOOR_MANAGER` roles
- [ ] **Audit log** — Persist login/logout/role-change events for regulatory compliance
- [ ] **Secrets management** — Move `JWT_PRIVATE_KEY_PEM` to AWS KMS or HashiCorp Vault in production
- [ ] **Token introspection endpoint** — `POST /introspect` for downstream services that cannot verify RS256 themselves
- [ ] **PIN complexity** — Enforce minimum entropy rules on player PINs
- [ ] **Scheduled revocation cleanup** — Add `@Scheduled` bean to call `deleteExpiredTokens` periodically
