-- ============================================================================
-- V9__seed_permissions.sql
-- ============================================================================

-- 1. Seed Permissions
INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES
    ('018f4e00-0000-7000-9000-000000000001', 'Permissions.Books.View', 'View Books', 'Books', 'Allows viewing books list and details', NOW()),
    ('018f4e00-0000-7000-9000-000000000002', 'Permissions.Books.Create', 'Create Book', 'Books', 'Allows creating new books', NOW()),
    ('018f4e00-0000-7000-9000-000000000003', 'Permissions.Books.Edit', 'Edit Book', 'Books', 'Allows editing existing books', NOW()),
    ('018f4e00-0000-7000-9000-000000000004', 'Permissions.Books.Delete', 'Delete Book', 'Books', 'Allows deleting books', NOW()),

    ('018f4e00-0000-7000-9000-000000000005', 'Permissions.Categories.View', 'View Categories', 'Categories', 'Allows viewing categories', NOW()),
    ('018f4e00-0000-7000-9000-000000000006', 'Permissions.Categories.Create', 'Create Category', 'Categories', 'Allows creating categories', NOW()),
    ('018f4e00-0000-7000-9000-000000000007', 'Permissions.Categories.Edit', 'Edit Category', 'Categories', 'Allows editing categories', NOW()),
    ('018f4e00-0000-7000-9000-000000000008', 'Permissions.Categories.Delete', 'Delete Category', 'Categories', 'Allows deleting categories', NOW()),

    ('018f4e00-0000-7000-9000-000000000009', 'Permissions.Carts.View', 'View Cart', 'Carts', 'Allows viewing active user cart', NOW()),
    ('018f4e00-0000-7000-9000-000000000010', 'Permissions.Carts.Modify', 'Modify Cart', 'Carts', 'Allows adding/removing/updating cart items', NOW()),

    ('018f4e00-0000-7000-9000-000000000011', 'Permissions.Users.View', 'View Users', 'Users', 'Allows viewing user profiles', NOW()),
    ('018f4e00-0000-7000-9000-000000000012', 'Permissions.Users.Edit', 'Edit Users', 'Users', 'Allows editing user accounts', NOW())
ON CONFLICT (permission_code) DO NOTHING;

-- 2. Link Admin Permissions (Role Code: ADMIN, Role ID: 018f4e00-0000-7000-8000-000000000001)
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT 
    -- Generate UUID based on md5 hash of concatenation of role_id and permission_id for idempotency
    CAST(md5('018f4e00-0000-7000-8000-000000000001' || id::text) AS UUID),
    '018f4e00-0000-7000-8000-000000000001', 
    id, 
    NOW()
FROM permissions
ON CONFLICT DO NOTHING;

-- 3. Link Customer Permissions (Role Code: CUSTOMER, Role ID: 018f4e00-0000-7000-8000-000000000002)
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT 
    CAST(md5('018f4e00-0000-7000-8000-000000000002' || id::text) AS UUID),
    '018f4e00-0000-7000-8000-000000000002', 
    id, 
    NOW()
FROM permissions
WHERE permission_code IN (
    'Permissions.Books.View',
    'Permissions.Categories.View',
    'Permissions.Carts.View',
    'Permissions.Carts.Modify'
)
ON CONFLICT DO NOTHING;
