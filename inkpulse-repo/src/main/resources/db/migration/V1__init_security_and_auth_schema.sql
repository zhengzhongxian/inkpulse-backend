-- ============================================================================
-- ROLLBACK (thứ tự DROP ngược với CREATE):
--   DROP TABLE IF EXISTS account_lock_logs CASCADE;
--   DROP TABLE IF EXISTS password_histories CASCADE;
--   DROP TABLE IF EXISTS security_questions CASCADE;
--   DROP TABLE IF EXISTS user_devices CASCADE;
--   DROP TABLE IF EXISTS mfa_configs CASCADE;
--   DROP TABLE IF EXISTS auth_providers CASCADE;
--   DROP TABLE IF EXISTS user_permissions CASCADE;
--   DROP TABLE IF EXISTS role_permissions CASCADE;
--   DROP TABLE IF EXISTS user_roles CASCADE;
--   DROP TABLE IF EXISTS user_settings CASCADE;
--   DROP TABLE IF EXISTS user_profiles CASCADE;
--   DROP TABLE IF EXISTS mfa_types CASCADE;
--   DROP TABLE IF EXISTS master_security_questions CASCADE;
--   DROP TABLE IF EXISTS permissions CASCADE;
--   DROP TABLE IF EXISTS roles CASCADE;
--   DROP TABLE IF EXISTS users CASCADE;
-- ============================================================================

-- 1. users
CREATE TABLE IF NOT EXISTS users (
    id                UUID PRIMARY KEY,
    user_name         VARCHAR(100)  NOT NULL,
    password          VARCHAR(255)  NOT NULL,
    password_change_at TIMESTAMP,
    email             VARCHAR(255)  NOT NULL,
    status            VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    is_verified       BOOLEAN       NOT NULL DEFAULT FALSE,
    phone_number      VARCHAR(20),
    mfa_enabled       BOOLEAN       NOT NULL DEFAULT FALSE,
    last_login_at     TIMESTAMP,
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN       NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS UQ_users_user_name ON users(user_name);
CREATE UNIQUE INDEX IF NOT EXISTS UQ_users_email ON users(email);
CREATE INDEX IF NOT EXISTS IDX_users_username ON users(user_name);
CREATE INDEX IF NOT EXISTS IDX_users_email ON users(email);

-- 2. roles
CREATE TABLE IF NOT EXISTS roles (
    id          UUID PRIMARY KEY,
    role_name   VARCHAR(100)  NOT NULL,
    role_code   VARCHAR(100)  NOT NULL,
    priority    INT           NOT NULL DEFAULT 0,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    description VARCHAR(500),
    created_at  TIMESTAMP     NOT NULL,
    created_by  UUID,
    updated_by  UUID,
    updated_at  TIMESTAMP,
    is_deleted  BOOLEAN       NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS UQ_roles_role_code ON roles(role_code);
CREATE INDEX IF NOT EXISTS IDX_roles_role_code ON roles(role_code);

-- 3. permissions
CREATE TABLE IF NOT EXISTS permissions (
    id              UUID PRIMARY KEY,
    permission_code VARCHAR(100)  NOT NULL,
    permission_name VARCHAR(100)  NOT NULL,
    module          VARCHAR(100)  NOT NULL,
    description     VARCHAR(500),
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS UQ_permissions_permission_code ON permissions(permission_code);
CREATE INDEX IF NOT EXISTS IDX_permissions_permission_code ON permissions(permission_code);

-- 4. master_security_questions
CREATE TABLE IF NOT EXISTS master_security_questions (
    id            UUID PRIMARY KEY,
    question_text VARCHAR(500)  NOT NULL,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL,
    created_by    UUID,
    updated_by    UUID,
    updated_at    TIMESTAMP,
    is_deleted    BOOLEAN       NOT NULL DEFAULT FALSE
);

-- 5. mfa_types
CREATE TABLE IF NOT EXISTS mfa_types (
    id           UUID PRIMARY KEY,
    type_name    VARCHAR(100)  NOT NULL,
    display_name VARCHAR(200)  NOT NULL,
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP     NOT NULL,
    updated_at   TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS UQ_mfa_types_type_name ON mfa_types(type_name);

-- 6. user_profiles
CREATE TABLE IF NOT EXISTS user_profiles (
    id          UUID PRIMARY KEY,
    user_id     UUID           NOT NULL,
    first_name  VARCHAR(100),
    last_name   VARCHAR(100),
    full_name   VARCHAR(200),
    gender      VARCHAR(50),
    dob         DATE,
    avatar_url  VARCHAR(500),
    biography   VARCHAR(1000),
    timezone    VARCHAR(100),
    created_at  TIMESTAMP      NOT NULL,
    updated_at  TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS UQ_user_profiles_user_id ON user_profiles(user_id);
ALTER TABLE user_profiles
    DROP CONSTRAINT IF EXISTS FK_user_profiles_users_user_id,
    ADD CONSTRAINT FK_user_profiles_users_user_id
        FOREIGN KEY (user_id) REFERENCES users(id);

-- 7. user_settings
CREATE TABLE IF NOT EXISTS user_settings (
    id              UUID PRIMARY KEY,
    user_id         UUID           NOT NULL,
    display_mode    VARCHAR(50)    NOT NULL DEFAULT 'SYSTEM',
    choice_language VARCHAR(50)    NOT NULL DEFAULT 'VI',
    created_at      TIMESTAMP      NOT NULL,
    updated_at      TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS UQ_user_settings_user_id ON user_settings(user_id);
ALTER TABLE user_settings
    DROP CONSTRAINT IF EXISTS FK_user_settings_users_user_id,
    ADD CONSTRAINT FK_user_settings_users_user_id
        FOREIGN KEY (user_id) REFERENCES users(id);

-- 8. user_roles
CREATE TABLE IF NOT EXISTS user_roles (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    role_id    UUID NOT NULL,
    expired_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS IDX_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS IDX_user_roles_role_id ON user_roles(role_id);
ALTER TABLE user_roles
    DROP CONSTRAINT IF EXISTS FK_user_roles_users_user_id,
    ADD CONSTRAINT FK_user_roles_users_user_id
        FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_roles
    DROP CONSTRAINT IF EXISTS FK_user_roles_roles_role_id,
    ADD CONSTRAINT FK_user_roles_roles_role_id
        FOREIGN KEY (role_id) REFERENCES roles(id);

-- 9. role_permissions
CREATE TABLE IF NOT EXISTS role_permissions (
    id            UUID PRIMARY KEY,
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS IDX_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS IDX_role_permissions_permission_id ON role_permissions(permission_id);
ALTER TABLE role_permissions
    DROP CONSTRAINT IF EXISTS FK_role_permissions_roles_role_id,
    ADD CONSTRAINT FK_role_permissions_roles_role_id
        FOREIGN KEY (role_id) REFERENCES roles(id);
ALTER TABLE role_permissions
    DROP CONSTRAINT IF EXISTS FK_role_permissions_permissions_permission_id,
    ADD CONSTRAINT FK_role_permissions_permissions_permission_id
        FOREIGN KEY (permission_id) REFERENCES permissions(id);

-- 10. user_permissions
CREATE TABLE IF NOT EXISTS user_permissions (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS IDX_user_permissions_user_id ON user_permissions(user_id);
CREATE INDEX IF NOT EXISTS IDX_user_permissions_permission_id ON user_permissions(permission_id);
ALTER TABLE user_permissions
    DROP CONSTRAINT IF EXISTS FK_user_permissions_users_user_id,
    ADD CONSTRAINT FK_user_permissions_users_user_id
        FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_permissions
    DROP CONSTRAINT IF EXISTS FK_user_permissions_permissions_permission_id,
    ADD CONSTRAINT FK_user_permissions_permissions_permission_id
        FOREIGN KEY (permission_id) REFERENCES permissions(id);

-- 11. auth_providers
CREATE TABLE IF NOT EXISTS auth_providers (
    id                   UUID PRIMARY KEY,
    user_id              UUID           NOT NULL,
    provider_name        VARCHAR(100)   NOT NULL,
    provider_subject_id  VARCHAR(255)   NOT NULL,
    created_at           TIMESTAMP      NOT NULL,
    updated_at           TIMESTAMP
);

CREATE INDEX IF NOT EXISTS IDX_auth_providers_user_id ON auth_providers(user_id);
ALTER TABLE auth_providers
    DROP CONSTRAINT IF EXISTS FK_auth_providers_users_user_id,
    ADD CONSTRAINT FK_auth_providers_users_user_id
        FOREIGN KEY (user_id) REFERENCES users(id);

-- 12. mfa_configs
CREATE TABLE IF NOT EXISTS mfa_configs (
    id         UUID PRIMARY KEY,
    user_id    UUID      NOT NULL,
    type_id    UUID      NOT NULL,
    is_default BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS IDX_mfa_configs_user_id ON mfa_configs(user_id);
ALTER TABLE mfa_configs
    DROP CONSTRAINT IF EXISTS FK_mfa_configs_users_user_id,
    ADD CONSTRAINT FK_mfa_configs_users_user_id
        FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE mfa_configs
    DROP CONSTRAINT IF EXISTS FK_mfa_configs_mfa_types_type_id,
    ADD CONSTRAINT FK_mfa_configs_mfa_types_type_id
        FOREIGN KEY (type_id) REFERENCES mfa_types(id);

-- 13. user_devices
CREATE TABLE IF NOT EXISTS user_devices (
    id                  UUID PRIMARY KEY,
    user_id             UUID           NOT NULL,
    device_name         VARCHAR(200)   NOT NULL,
    device_type         VARCHAR(50)    NOT NULL,
    browser_fingerprint VARCHAR(255),
    last_login_at       TIMESTAMP,
    is_trusted          BOOLEAN        NOT NULL DEFAULT FALSE,
    trust_until         TIMESTAMP,
    created_at          TIMESTAMP      NOT NULL,
    updated_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS IDX_user_devices_user_id ON user_devices(user_id);
ALTER TABLE user_devices
    DROP CONSTRAINT IF EXISTS FK_user_devices_users_user_id,
    ADD CONSTRAINT FK_user_devices_users_user_id
        FOREIGN KEY (user_id) REFERENCES users(id);

-- 14. security_questions
CREATE TABLE IF NOT EXISTS security_questions (
    id          UUID PRIMARY KEY,
    user_id     UUID           NOT NULL,
    question_id UUID           NOT NULL,
    answer_hash VARCHAR(255)   NOT NULL,
    created_at  TIMESTAMP      NOT NULL,
    updated_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS IDX_security_questions_user_id ON security_questions(user_id);
ALTER TABLE security_questions
    DROP CONSTRAINT IF EXISTS FK_security_questions_users_user_id,
    ADD CONSTRAINT FK_security_questions_users_user_id
        FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE security_questions
    DROP CONSTRAINT IF EXISTS FK_security_questions_master_security_questions_question_id,
    ADD CONSTRAINT FK_security_questions_master_security_questions_question_id
        FOREIGN KEY (question_id) REFERENCES master_security_questions(id);

-- 15. password_histories
CREATE TABLE IF NOT EXISTS password_histories (
    id         UUID PRIMARY KEY,
    user_id    UUID           NOT NULL,
    password   VARCHAR(255)   NOT NULL,
    changed_at TIMESTAMP      NOT NULL,
    changed_by UUID,
    created_at TIMESTAMP      NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS IDX_password_histories_user_id ON password_histories(user_id);
ALTER TABLE password_histories
    DROP CONSTRAINT IF EXISTS FK_password_histories_users_user_id,
    ADD CONSTRAINT FK_password_histories_users_user_id
        FOREIGN KEY (user_id) REFERENCES users(id);

-- 16. account_lock_logs
CREATE TABLE IF NOT EXISTS account_lock_logs (
    id          UUID PRIMARY KEY,
    user_id     UUID           NOT NULL,
    reason_code VARCHAR(100)   NOT NULL,
    locked_at   TIMESTAMP      NOT NULL,
    unlock_at   TIMESTAMP,
    unlocked_at TIMESTAMP,
    unlocked_by UUID,
    ip_address  VARCHAR(45),
    note        VARCHAR(500),
    created_at  TIMESTAMP      NOT NULL,
    updated_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS IDX_account_lock_logs_user_id ON account_lock_logs(user_id);
ALTER TABLE account_lock_logs
    DROP CONSTRAINT IF EXISTS FK_account_lock_logs_users_user_id,
    ADD CONSTRAINT FK_account_lock_logs_users_user_id
        FOREIGN KEY (user_id) REFERENCES users(id);
