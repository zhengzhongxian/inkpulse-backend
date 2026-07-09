INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES ('019f4e00-0000-7000-9000-000000000111', 'Permissions.Orders.InternalView', 
        'Internal View Orders', 'Orders', 'Allows viewing internal orders list, detail and logs', NOW())
ON CONFLICT (permission_code) DO NOTHING;

-- Link to ADMIN (Role ID: 018f4e00-0000-7000-8000-000000000001)
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT CAST(md5('018f4e00-0000-7000-8000-000000000001' || id::text) AS UUID),
       '018f4e00-0000-7000-8000-000000000001', id, NOW()
FROM permissions WHERE permission_code = 'Permissions.Orders.InternalView'
ON CONFLICT DO NOTHING;
