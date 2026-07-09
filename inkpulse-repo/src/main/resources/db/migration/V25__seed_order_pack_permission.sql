-- ============================================================================
-- V25__seed_order_pack_permission.sql
-- ============================================================================

-- 1. Seed Order Pack Permission
INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES
    ('019f4e00-0000-7000-9000-000000000110', 'Permissions.Orders.Pack', 'Pack Order', 'Orders', 'Allows confirming order packaging and creating GHN shipment', NOW())
ON CONFLICT (permission_code) DO NOTHING;

-- 2. Link to ADMIN role (Role ID: 018f4e00-0000-7000-8000-000000000001)
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT 
    CAST(md5('018f4e00-0000-7000-8000-000000000001' || id::text) AS UUID),
    '018f4e00-0000-7000-8000-000000000001', 
    id, 
    NOW()
FROM permissions
WHERE permission_code = 'Permissions.Orders.Pack'
ON CONFLICT DO NOTHING;
