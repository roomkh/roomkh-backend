-- ============================================================================
-- V2: users + refresh_tokens (rotating, hashed, family-based reuse detection)
-- ============================================================================

CREATE TABLE users (
    id                   BIGSERIAL PRIMARY KEY,
    uuid                 UUID NOT NULL DEFAULT gen_random_uuid(),
    full_name            VARCHAR(120) NOT NULL,
    email                VARCHAR(190) NOT NULL,
    phone                VARCHAR(32),
    phone_verified_at    TIMESTAMPTZ,
    password_hash        VARCHAR(255),
    avatar_url           TEXT,
    preferred_language   VARCHAR(8) NOT NULL DEFAULT 'KM' CHECK (preferred_language IN ('KM', 'EN')),
    role                 VARCHAR(16) NOT NULL DEFAULT 'USER'
                         CHECK (role IN ('USER', 'OWNER', 'AGENT', 'MODERATOR', 'ADMIN')),
    status               VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
                         CHECK (status IN ('ACTIVE', 'SUSPENDED', 'BANNED')),
    provider             VARCHAR(16) NOT NULL DEFAULT 'LOCAL'
                         CHECK (provider IN ('LOCAL', 'GOOGLE', 'FACEBOOK')),
    provider_id          VARCHAR(190),
    last_login_at        TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ,
    CONSTRAINT uq_users_uuid UNIQUE (uuid),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone)
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_phone ON users (phone);
CREATE INDEX idx_users_uuid ON users (uuid);

CREATE TABLE refresh_tokens (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash   VARCHAR(64) NOT NULL,
    family_id    UUID NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    revoked_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ,
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
