-- V20__add_social_accounts_table_and_alter_password_nullable.sql

-- 1. Alter users.password to be nullable
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- 2. Create user_social_accounts table
CREATE TABLE IF NOT EXISTS user_social_accounts (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL,
    provider      VARCHAR(50) NOT NULL,
    provider_key  VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_user_social_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Create indices for performance and uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS UQ_user_social_accounts_provider_key ON user_social_accounts(provider, provider_key);
CREATE INDEX IF NOT EXISTS IDX_user_social_accounts_user_id ON user_social_accounts(user_id);
