-- ============================================================================
-- V18__seed_author_write_permissions.sql
-- ============================================================================

-- 1. Seed Permissions
INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES
    ('019f4e00-0000-7000-9000-000000000106', 'Permissions.Authors.Create', 'Create Authors', 'Authors', 'Allows creating new authors', NOW()),
    ('019f4e00-0000-7000-9000-000000000107', 'Permissions.Authors.Edit', 'Edit Authors', 'Authors', 'Allows editing existing authors', NOW()),
    ('019f4e00-0000-7000-9000-000000000108', 'Permissions.Authors.Delete', 'Delete Authors', 'Authors', 'Allows deleting authors', NOW())
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
    'Permissions.Authors.Create',
    'Permissions.Authors.Edit',
    'Permissions.Authors.Delete'
)
ON CONFLICT DO NOTHING;
