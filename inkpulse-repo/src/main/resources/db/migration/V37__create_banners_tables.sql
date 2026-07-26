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
INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES
    (gen_random_uuid(), 'Permissions.Banners.View', 'Xem danh sách banner nội bộ', 'Banner', 'Allows viewing internal banner list and details', NOW()),
    (gen_random_uuid(), 'Permissions.Banners.Create', 'Tạo banner quảng cáo mới', 'Banner', 'Allows creating new banner campaigns', NOW()),
    (gen_random_uuid(), 'Permissions.Banners.Edit', 'Chỉnh sửa banner quảng cáo', 'Banner', 'Allows updating existing banners', NOW()),
    (gen_random_uuid(), 'Permissions.Banners.Delete', 'Xóa banner quảng cáo', 'Banner', 'Allows deleting banner campaigns', NOW())
ON CONFLICT (permission_code) DO NOTHING;

-- Grant banner permissions to ADMIN role
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '018f4e00-0000-7000-8000-000000000001', id, NOW()
FROM permissions
WHERE permission_code IN (
    'Permissions.Banners.View',
    'Permissions.Banners.Create',
    'Permissions.Banners.Edit',
    'Permissions.Banners.Delete'
)
ON CONFLICT DO NOTHING;
