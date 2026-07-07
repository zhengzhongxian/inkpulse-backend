-- V22__replace_dimensions_with_shipping_fields.sql

-- 1. Add shipping fields to book_editions
ALTER TABLE book_editions ADD COLUMN weight_gram INT NOT NULL DEFAULT 500;
ALTER TABLE book_editions ADD COLUMN width_cm INT NOT NULL DEFAULT 20;
ALTER TABLE book_editions ADD COLUMN height_cm INT NOT NULL DEFAULT 3;
ALTER TABLE book_editions ADD COLUMN length_cm INT NOT NULL DEFAULT 15;

-- 2. Migrate existing records (estimating weight based on page count if available)
UPDATE book_editions SET weight_gram = COALESCE(page_count, 300) + 200;

-- 3. Drop old dimensions column
ALTER TABLE book_editions DROP COLUMN IF EXISTS dimensions;

-- 4. Version columns for concurrency control (entity has it, V21 migration forgot to add to DDL)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE orders_detail ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE order_logs ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
