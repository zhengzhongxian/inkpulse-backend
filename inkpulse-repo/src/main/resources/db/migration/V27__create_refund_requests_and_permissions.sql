CREATE TABLE refund_requests (
    refund_request_id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(order_id),
    amount NUMERIC(19, 4) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason TEXT,
    approved_by UUID REFERENCES users(id),
    payos_refund_id VARCHAR(100),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0
);

-- Seed permissions
INSERT INTO permissions (id, permission_code, permission_name, module, description, created_at)
VALUES 
('019f4e00-0000-7000-9000-000000000201', 'Permissions.Orders.Cancel', 'Cancel Order', 'Orders', 'Allows canceling order before shipment', NOW()),
('019f4e00-0000-7000-9000-000000000202', 'Permissions.Orders.Return', 'Return Order', 'Orders', 'Allows requesting return/refund on shipped order', NOW()),
('019f4e00-0000-7000-9000-000000000203', 'Permissions.Refunds.View', 'View Refund Requests', 'Refunds', 'Allows viewing refund request lists and details', NOW()),
('019f4e00-0000-7000-9000-000000000204', 'Permissions.Refunds.Approve', 'Approve Refund Requests', 'Refunds', 'Allows approving and processing refund requests via PayOS', NOW())
ON CONFLICT (permission_code) DO NOTHING;

-- Link new permissions to ADMIN role (018f4e00-0000-7000-8000-000000000001)
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT CAST(md5('018f4e00-0000-7000-8000-000000000001' || id::text) AS UUID),
       '018f4e00-0000-7000-8000-000000000001', id, NOW()
FROM permissions 
WHERE permission_code IN (
    'Permissions.Orders.Cancel', 
    'Permissions.Orders.Return', 
    'Permissions.Refunds.View', 
    'Permissions.Refunds.Approve'
)
ON CONFLICT DO NOTHING;
