-- ============================================================================
-- V12__seed_internal_permissions.sql
-- ============================================================================

-- 1. Seed Permissions
INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES
    ('018f4e00-0000-7000-9000-000000000013', 'Permissions.Books.InternalView', 'Internal View Books', 'Books', 'Allows internal books list and details without search indices', NOW()),
    ('018f4e00-0000-7000-9000-000000000014', 'Permissions.Authors.InternalView', 'Internal View Authors', 'Authors', 'Allows internal authors list and details directly from DB', NOW()),
    ('018f4e00-0000-7000-9000-000000000015', 'Permissions.Auth.InternalLogin', 'Internal Login', 'Auth', 'Allows logging into management interfaces directly without MFA', NOW()),
    ('018f4e00-0000-7000-9000-000000000016', 'Permissions.Authors.View', 'View Authors', 'Authors', 'Allows viewing author list and details', NOW())
ON CONFLICT (permission_code) DO NOTHING;

-- 2. Link Admin Permissions (Role Code: ADMIN, Role ID: 018f4e00-0000-7000-8000-000000000001)
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT 
    CAST(md5('018f4e00-0000-7000-8000-000000000001' || id::text) AS UUID),
    '018f4e00-0000-7000-8000-000000000001', 
    id, 
    NOW()
FROM permissions
WHERE permission_code IN (
    'Permissions.Books.InternalView',
    'Permissions.Authors.InternalView',
    'Permissions.Auth.InternalLogin',
    'Permissions.Authors.View'
)
ON CONFLICT DO NOTHING;

-- 3. Link Customer Permissions (Role Code: CUSTOMER, Role ID: 018f4e00-0000-7000-8000-000000000002)
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT 
    CAST(md5('018f4e00-0000-7000-8000-000000000002' || id::text) AS UUID),
    '018f4e00-0000-7000-8000-000000000002', 
    id, 
    NOW()
FROM permissions
WHERE permission_code = 'Permissions.Authors.View'
ON CONFLICT DO NOTHING;
