-- V33__create_flash_sales_table.sql
CREATE TABLE IF NOT EXISTS flash_sales (
    flash_sale_id UUID PRIMARY KEY,
    book_edition_id UUID NOT NULL REFERENCES book_editions(id) ON DELETE CASCADE,
    discount_amount NUMERIC(19, 4) NOT NULL,
    flash_sale_stock INT NOT NULL,
    sold_count INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_flash_sales_active_dates ON flash_sales (is_active, is_deleted, start_date, end_date);
