-- ============================================================================
-- V13__seed_badge_permissions_and_data.sql
-- ============================================================================

-- 1. Seed Permissions
INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES
    ('018f4e00-0000-7000-9000-000000000021', 'Permissions.Badges.View', 'View Badges', 'Badges', 'Allows viewing badge list and details', NOW()),
    ('018f4e00-0000-7000-9000-000000000022', 'Permissions.Badges.Create', 'Create Badges', 'Badges', 'Allows creating new badges', NOW()),
    ('018f4e00-0000-7000-9000-000000000023', 'Permissions.Badges.Edit', 'Edit Badges', 'Badges', 'Allows editing existing badges', NOW()),
    ('018f4e00-0000-7000-9000-000000000024', 'Permissions.Badges.Delete', 'Delete Badges', 'Badges', 'Allows deleting badges', NOW()),
    ('018f4e00-0000-7000-9000-000000000025', 'Permissions.Badges.InternalView', 'Internal View Badges', 'Badges', 'Allows internal listing of badges', NOW())
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
    'Permissions.Badges.View',
    'Permissions.Badges.Create',
    'Permissions.Badges.Edit',
    'Permissions.Badges.Delete',
    'Permissions.Badges.InternalView'
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
WHERE permission_code = 'Permissions.Badges.View'
ON CONFLICT DO NOTHING;

-- 4. Seed Badge Data (using UUID v7 values)
INSERT INTO badges (id, text, text_color, bg_color, is_deleted, created_at, updated_at, version)
VALUES
    ('018f4e00-0000-7000-a000-000000000001', 'Bán Chạy', '#FFFFFF', '#ef4444', false, NOW(), NOW(), 0),
    ('018f4e00-0000-7000-a000-000000000002', 'Mới Nhất', '#FFFFFF', '#3b82f6', false, NOW(), NOW(), 0),
    ('018f4e00-0000-7000-a000-000000000003', 'Giảm Giá', '#FFFFFF', '#eab308', false, NOW(), NOW(), 0),
    ('018f4e00-0000-7000-a000-000000000004', 'Gợi Ý', '#FFFFFF', '#10b981', false, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;
