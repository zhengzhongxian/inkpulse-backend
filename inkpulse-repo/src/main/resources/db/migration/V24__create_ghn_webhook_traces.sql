-- ============================================================================
-- V24__create_ghn_webhook_traces.sql
-- ============================================================================

CREATE TABLE ghn_webhook_traces (
    id BIGSERIAL PRIMARY KEY,
    order_code VARCHAR(50) NOT NULL,
    ghn_status VARCHAR(50) NOT NULL,
    raw_payload TEXT NOT NULL,
    processed_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE UNIQUE INDEX uidx_ghn_trace_order_status 
    ON ghn_webhook_traces(order_code, ghn_status) 
    WHERE processed_status = 'SUCCESS';
