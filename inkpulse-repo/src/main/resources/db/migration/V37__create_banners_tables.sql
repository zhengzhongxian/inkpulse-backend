-- Migration V37: Create banners and banner_editions tables and seed permissions

CREATE TABLE IF NOT EXISTS banners (
    banner_id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    subtitle TEXT,
    image_url VARCHAR(500) NOT NULL,
    icon_url VARCHAR(500),
    link_url VARCHAR(500),
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS banner_editions (
    banner_edition_id UUID PRIMARY KEY,
    banner_id UUID NOT NULL REFERENCES banners(banner_id) ON DELETE CASCADE,
    edition_id UUID NOT NULL REFERENCES book_editions(id) ON DELETE CASCADE,
    display_order INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_banners_is_active_order ON banners(is_active, display_order) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_banner_editions_banner_id ON banner_editions(banner_id);

-- Seed permissions for Banners
INSERT INTO permissions (permission_id, name, code, module, created_at, updated_at, is_deleted, version)
VALUES
    (gen_random_uuid(), 'Xem danh sách banner nội bộ', 'Permissions.Banners.View', 'Banner', NOW(), NOW(), false, 0),
    (gen_random_uuid(), 'Tạo banner quảng cáo mới', 'Permissions.Banners.Create', 'Banner', NOW(), NOW(), false, 0),
    (gen_random_uuid(), 'Chỉnh sửa banner quảng cáo', 'Permissions.Banners.Edit', 'Banner', NOW(), NOW(), false, 0),
    (gen_random_uuid(), 'Xóa banner quảng cáo', 'Permissions.Banners.Delete', 'Banner', NOW(), NOW(), false, 0)
ON CONFLICT (code) DO NOTHING;

-- Grant banner permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.module = 'Banner'
ON CONFLICT DO NOTHING;
