-- V35__create_multi_product_flash_sales.sql
DROP TABLE IF EXISTS flash_sales CASCADE;

CREATE TABLE flash_sales (
    flash_sale_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE flash_sale_items (
    flash_sale_item_id UUID PRIMARY KEY,
    flash_sale_id UUID NOT NULL REFERENCES flash_sales(flash_sale_id) ON DELETE CASCADE,
    book_edition_id UUID NOT NULL REFERENCES book_editions(id) ON DELETE CASCADE,
    discount_amount NUMERIC(19, 4) NOT NULL,
    flash_sale_stock INT NOT NULL,
    sold_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_flash_sales_active_dates ON flash_sales (is_active, is_deleted, start_date, end_date);
CREATE INDEX idx_flash_sale_items_edition ON flash_sale_items (book_edition_id);
CREATE INDEX idx_flash_sale_items_campaign ON flash_sale_items (flash_sale_id);
