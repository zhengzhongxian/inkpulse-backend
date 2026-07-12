CREATE TABLE system_settings (
    system_setting_id UUID PRIMARY KEY,
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_system_settings_key ON system_settings(setting_key);

-- Seed default configs
INSERT INTO system_settings (system_setting_id, setting_key, setting_value, description, created_at, updated_at)
VALUES 
('019f4fcf-4013-7119-ad44-9f9c558d93d6', 'bonus_coins', '10', 'Số lượng xu thưởng nhận được cho mỗi 1000đ thanh toán đơn hàng', NOW(), NOW())
ON CONFLICT (setting_key) DO NOTHING;

-- Seed permissions
INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES 
('019f4fcf-4013-7119-ad44-9f9c558d9301', 'Permissions.SystemSettings.View', 'View System Settings', 'SystemSettings', 'Allows viewing system configurations', NOW()),
('019f4fcf-4013-7119-ad44-9f9c558d9302', 'Permissions.SystemSettings.Update', 'Update System Settings', 'SystemSettings', 'Allows modifying system configurations', NOW())
ON CONFLICT (permission_code) DO NOTHING;

-- Link new permissions to ADMIN role (018f4e00-0000-7000-8000-000000000001)
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT CAST(md5('018f4e00-0000-7000-8000-000000000001' || id::text) AS UUID),
       '018f4e00-0000-7000-8000-000000000001', id, NOW()
FROM permissions 
WHERE permission_code IN (
    'Permissions.SystemSettings.View', 
    'Permissions.SystemSettings.Update'
)
ON CONFLICT DO NOTHING;
