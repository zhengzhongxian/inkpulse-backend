-- V4: Seed mfa_types and assign EMAIL and PUSH mfa_configs for customer_demo

-- 1. Insert MFA Types
INSERT INTO mfa_types (id, type_name, display_name, is_active, created_at)
VALUES
    ('018f4e00-0000-7000-8000-000000000003', 'EMAIL', 'Email OTP', TRUE, NOW()),
    ('018f4e00-0000-7000-8000-000000000004', 'TOTP', 'Google Authenticator', TRUE, NOW()),
    ('018f4e00-0000-7000-8000-000000000005', 'PUSH', 'Push Notification', TRUE, NOW())
ON CONFLICT (type_name) DO UPDATE
SET display_name = EXCLUDED.display_name,
    is_active = EXCLUDED.is_active;

-- 2. Insert MFA configs for customer_demo (user_id = '018f4e00-0000-7000-8000-000000000011')
-- We will add EMAIL and PUSH verification methods.
INSERT INTO mfa_configs (id, user_id, type_id, is_default, created_at)
VALUES
    ('018f4e00-0000-7000-8000-000000000050', '018f4e00-0000-7000-8000-000000000011', '018f4e00-0000-7000-8000-000000000003', TRUE, NOW()),
    ('018f4e00-0000-7000-8000-000000000051', '018f4e00-0000-7000-8000-000000000011', '018f4e00-0000-7000-8000-000000000005', FALSE, NOW())
ON CONFLICT DO NOTHING;
