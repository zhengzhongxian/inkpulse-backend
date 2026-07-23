-- V34__seed_flash_sale_permissions.sql

INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES
  ('019f4fcf-5013-7119-ad44-9f9c558d9307', 'Permissions.FlashSales.View', 'View Flash Sales', 'FlashSales', 'Allows viewing internal flash sales list and details', NOW()),
  ('019f4fcf-5013-7119-ad44-9f9c558d9308', 'Permissions.FlashSales.Create', 'Create Flash Sale', 'FlashSales', 'Allows creating new flash sales campaigns', NOW()),
  ('019f4fcf-5013-7119-ad44-9f9c558d9309', 'Permissions.FlashSales.Edit', 'Edit Flash Sale', 'FlashSales', 'Allows updating existing flash sales', NOW()),
  ('019f4fcf-5013-7119-ad44-9f9c558d9310', 'Permissions.FlashSales.Delete', 'Delete Flash Sale', 'FlashSales', 'Allows deleting flash sales campaigns', NOW())
ON CONFLICT (permission_code) DO NOTHING;

-- Gán cho role Admin
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT CAST(md5('018f4e00-0000-7000-8000-000000000001' || id::text) AS UUID),
       '018f4e00-0000-7000-8000-000000000001', id, NOW()
FROM permissions
WHERE permission_code IN (
    'Permissions.FlashSales.View',
    'Permissions.FlashSales.Create',
    'Permissions.FlashSales.Edit',
    'Permissions.FlashSales.Delete'
)
ON CONFLICT DO NOTHING;
