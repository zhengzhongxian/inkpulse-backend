-- ============================================================================
-- V2: Seed initial roles and users (admin + customer)
--
-- BCrypt passwords:
--   ADMIN    : $2a$12$lQ9jTDFD0FJDY5omKiGfM.IEgSmeaxTyxIiskeigKb9D54SH9eEmi
--   CUSTOMER : $2a$12$lgT9akds0zunPgv16W62L.GCA73nZ.GZPOKohRejpW4tWd3QlZZPq
--
-- ROLLBACK:
--   DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE user_name IN ('admin','customer_demo'));
--   DELETE FROM user_settings WHERE user_id IN (SELECT id FROM users WHERE user_name IN ('admin','customer_demo'));
--   DELETE FROM user_profiles WHERE user_id IN (SELECT id FROM users WHERE user_name IN ('admin','customer_demo'));
--   DELETE FROM users WHERE user_name IN ('admin','customer_demo');
--   DELETE FROM roles WHERE role_code IN ('ADMIN','CUSTOMER');
-- ============================================================================

-- ─── 1. Roles ────────────────────────────────────────────────────────────────
INSERT INTO roles (id, role_name, role_code, priority, is_active, description, created_at, is_deleted)
VALUES
    ('018f4e00-0000-7000-8000-000000000001', 'Administrator', 'ADMIN',    100, TRUE, 'Full system access',   NOW(), FALSE),
    ('018f4e00-0000-7000-8000-000000000002', 'Customer',      'CUSTOMER',  10, TRUE, 'Regular customer role', NOW(), FALSE)
ON CONFLICT DO NOTHING;

-- ─── 2. Users ────────────────────────────────────────────────────────────────
INSERT INTO users (id, user_name, password, email, status, is_verified, mfa_enabled, created_at, is_deleted)
VALUES
    (
        '018f4e00-0000-7000-8000-000000000010',
        'admin',
        '$2a$12$lQ9jTDFD0FJDY5omKiGfM.IEgSmeaxTyxIiskeigKb9D54SH9eEmi',
        'admin@inkpulse.local',
        'ACTIVE',
        TRUE,
        FALSE,
        NOW(),
        FALSE
    ),
    (
        '018f4e00-0000-7000-8000-000000000011',
        'customer_demo',
        '$2a$12$lgT9akds0zunPgv16W62L.GCA73nZ.GZPOKohRejpW4tWd3QlZZPq',
        'trunghien765@gmail.com',
        'ACTIVE',
        TRUE,
        TRUE,
        NOW(),
        FALSE
    )
ON CONFLICT DO NOTHING;

-- ─── 3. Profiles ────────────────────────────────────────────────────────────
INSERT INTO user_profiles (id, user_id, first_name, last_name, full_name, created_at)
VALUES
    ('018f4e00-0000-7000-8000-000000000020', '018f4e00-0000-7000-8000-000000000010', 'System',   'Admin',    'System Admin',    NOW()),
    ('018f4e00-0000-7000-8000-000000000021', '018f4e00-0000-7000-8000-000000000011', 'Customer', 'Demo',     'Customer Demo',   NOW())
ON CONFLICT DO NOTHING;

-- ─── 4. Settings ────────────────────────────────────────────────────────────
INSERT INTO user_settings (id, user_id, display_mode, choice_language, created_at)
VALUES
    ('018f4e00-0000-7000-8000-000000000030', '018f4e00-0000-7000-8000-000000000010', 'SYSTEM', 'VI', NOW()),
    ('018f4e00-0000-7000-8000-000000000031', '018f4e00-0000-7000-8000-000000000011', 'SYSTEM', 'VI', NOW())
ON CONFLICT DO NOTHING;

-- ─── 5. User → Role assignments ──────────────────────────────────────────────
INSERT INTO user_roles (id, user_id, role_id, created_at)
VALUES
    ('018f4e00-0000-7000-8000-000000000040', '018f4e00-0000-7000-8000-000000000010', '018f4e00-0000-7000-8000-000000000001', NOW()),
    ('018f4e00-0000-7000-8000-000000000041', '018f4e00-0000-7000-8000-000000000011', '018f4e00-0000-7000-8000-000000000002', NOW())
ON CONFLICT DO NOTHING;
