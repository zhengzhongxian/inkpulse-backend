-- V30__seed_voucher_permissions.sql

INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES
  ('019f4fcf-4013-7119-ad44-9f9c558d9303', 'Permissions.Vouchers.View', 'View Vouchers', 'Vouchers', 'Allows viewing internal vouchers list and details', NOW()),
  ('019f4fcf-4013-7119-ad44-9f9c558d9304', 'Permissions.Vouchers.Create', 'Create Voucher', 'Vouchers', 'Allows creating new promotional vouchers', NOW()),
  ('019f4fcf-4013-7119-ad44-9f9c558d9305', 'Permissions.Vouchers.Edit', 'Edit Voucher', 'Vouchers', 'Allows updating existing vouchers', NOW()),
  ('019f4fcf-4013-7119-ad44-9f9c558d9306', 'Permissions.Vouchers.Delete', 'Delete Voucher', 'Vouchers', 'Allows deleting vouchers', NOW())
ON CONFLICT (permission_code) DO NOTHING;

-- Gán cho role Admin
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT CAST(md5('018f4e00-0000-7000-8000-000000000001' || id::text) AS UUID),
       '018f4e00-0000-7000-8000-000000000001', id, NOW()
FROM permissions
WHERE permission_code IN (
    'Permissions.Vouchers.View',
    'Permissions.Vouchers.Create',
    'Permissions.Vouchers.Edit',
    'Permissions.Vouchers.Delete'
)
ON CONFLICT DO NOTHING;
