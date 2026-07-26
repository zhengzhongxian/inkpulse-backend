-- Migration V38: Fix banner_editions schema and update banner permissions to Enterprise standard

-- 1. Ensure banner_editions table exists with proper foreign key reference to book_editions(id)
CREATE TABLE IF NOT EXISTS banner_editions (
    banner_edition_id UUID PRIMARY KEY,
    banner_id UUID NOT NULL REFERENCES banners(banner_id) ON DELETE CASCADE,
    edition_id UUID NOT NULL REFERENCES book_editions(id) ON DELETE CASCADE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Add missing columns if table already existed from V37
ALTER TABLE banner_editions ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE banner_editions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE banner_editions ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Seed standardized Enterprise permission codes
INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES
    (gen_random_uuid(), 'Permissions.Banners.View', 'Xem danh sách banner nội bộ', 'Banner', 'Allows viewing internal banner list and details', NOW()),
    (gen_random_uuid(), 'Permissions.Banners.Create', 'Tạo banner quảng cáo mới', 'Banner', 'Allows creating new banner campaigns', NOW()),
    (gen_random_uuid(), 'Permissions.Banners.Edit', 'Chỉnh sửa banner quảng cáo', 'Banner', 'Allows updating existing banners', NOW()),
    (gen_random_uuid(), 'Permissions.Banners.Delete', 'Xóa banner quảng cáo', 'Banner', 'Allows deleting banner campaigns', NOW())
ON CONFLICT (permission_code) DO NOTHING;

-- 3. Grant new permissions to ADMIN role
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
