-- 1. Order Event Store (append-only, immutable but inherits BaseAuditableEntity fields)
CREATE TABLE order_events (
    event_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES orders(order_id),
    event_type      VARCHAR(50) NOT NULL,
    event_data      JSONB,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_order_events_order_id ON order_events(order_id);
CREATE INDEX idx_order_events_type ON order_events(event_type);

-- 2. Stock Transaction Event Store (append-only, immutable but inherits BaseAuditableEntity fields)
CREATE TABLE stock_transactions (
    transaction_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    edition_id      UUID NOT NULL REFERENCES book_editions(id),
    delta           INTEGER NOT NULL,
    type            VARCHAR(50) NOT NULL,
    reference_code  VARCHAR(100),
    note            TEXT,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_stock_tx_edition ON stock_transactions(edition_id);
CREATE INDEX idx_stock_tx_type ON stock_transactions(type);

-- 3. Migration: Tạo INITIAL_STOCK events cho các editions hiện tại có tồn kho > 0
INSERT INTO stock_transactions (transaction_id, edition_id, delta, type, reference_code, note, created_by, created_at, updated_at, is_deleted)
SELECT gen_random_uuid(), id, stock_quantity, 'INITIAL_STOCK',
       'MIGRATION_V29', 'Khởi tạo tồn kho ban đầu từ dữ liệu hiện có',
       '00000000-0000-0000-0000-000000000000', NOW(), NOW(), FALSE
FROM book_editions
WHERE is_deleted = false AND stock_quantity > 0;

-- 4. Seed new permission for Inventory Management
INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES 
('019f4fcf-4013-7119-ad44-9f9c558d9401', 'Permissions.Inventory.Manage', 'Manage Inventory', 'Inventory', 'Allows manual stock import and adjustment', NOW())
ON CONFLICT (permission_code) DO NOTHING;

-- 5. Link new permission to ADMIN role (018f4e00-0000-7000-8000-000000000001)
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT CAST(md5('018f4e00-0000-7000-8000-000000000001' || id::text) AS UUID),
       '018f4e00-0000-7000-8000-000000000001', id, NOW()
FROM permissions 
WHERE permission_code = 'Permissions.Inventory.Manage'
ON CONFLICT DO NOTHING;
