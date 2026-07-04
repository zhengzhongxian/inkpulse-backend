-- ============================================================================
-- V14__seed_publisher_permissions.sql
-- ============================================================================

-- 1. Seed Permissions
INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES
    ('018f4e00-0000-7000-9000-000000000031', 'Permissions.Publishers.View', 'View Publishers', 'Publishers', 'Allows viewing publisher list and details', NOW()),
    ('018f4e00-0000-7000-9000-000000000032', 'Permissions.Publishers.Create', 'Create Publishers', 'Publishers', 'Allows creating new publishers', NOW()),
    ('018f4e00-0000-7000-9000-000000000033', 'Permissions.Publishers.Edit', 'Edit Publishers', 'Publishers', 'Allows editing existing publishers', NOW()),
    ('018f4e00-0000-7000-9000-000000000034', 'Permissions.Publishers.Delete', 'Delete Publishers', 'Publishers', 'Allows deleting publishers', NOW()),
    ('018f4e00-0000-7000-9000-000000000035', 'Permissions.Publishers.InternalView', 'Internal View Publishers', 'Publishers', 'Allows internal listing of publishers', NOW())
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
    'Permissions.Publishers.View',
    'Permissions.Publishers.Create',
    'Permissions.Publishers.Edit',
    'Permissions.Publishers.Delete',
    'Permissions.Publishers.InternalView'
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
WHERE permission_code = 'Permissions.Publishers.View'
ON CONFLICT DO NOTHING;
