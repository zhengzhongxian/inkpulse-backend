-- ============================================================================
-- V17__seed_category_permissions.sql
-- ============================================================================

INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES
    ('019f4e00-0000-7000-9000-000000000101', 'Permissions.Categories.View', 'View Categories', 'Categories', 'Allows viewing category list and details', NOW()),
    ('019f4e00-0000-7000-9000-000000000102', 'Permissions.Categories.Create', 'Create Categories', 'Categories', 'Allows creating new categories', NOW()),
    ('019f4e00-0000-7000-9000-000000000103', 'Permissions.Categories.Edit', 'Edit Categories', 'Categories', 'Allows editing existing categories', NOW()),
    ('019f4e00-0000-7000-9000-000000000104', 'Permissions.Categories.Delete', 'Delete Categories', 'Categories', 'Allows deleting categories', NOW()),
    ('019f4e00-0000-7000-9000-000000000105', 'Permissions.Categories.InternalView', 'Internal View Categories', 'Categories', 'Allows internal listing of all categories', NOW())
ON CONFLICT (permission_code) DO NOTHING;

-- Link Category permissions to ADMIN role (Role ID: 018f4e00-0000-7000-8000-000000000001)
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT 
    CAST(md5('018f4e00-0000-7000-8000-000000000001' || id::text) AS UUID),
    '018f4e00-0000-7000-8000-000000000001', 
    id, 
    NOW()
FROM permissions
WHERE permission_code IN (
    'Permissions.Categories.View',
    'Permissions.Categories.Create',
    'Permissions.Categories.Edit',
    'Permissions.Categories.Delete',
    'Permissions.Categories.InternalView'
)
ON CONFLICT DO NOTHING;