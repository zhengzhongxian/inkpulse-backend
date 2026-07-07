-- V21__create_ordering_and_vouchers_tables.sql

-- 1. Thêm cột số dư xu vào bảng user_profiles
ALTER TABLE user_profiles ADD COLUMN coin_balance bigint NOT NULL DEFAULT 0;

-- 2. Tăng độ dài số điện thoại để lưu chuỗi AES mã hóa
ALTER TABLE user_addresses ALTER COLUMN recipient_phone TYPE varchar(255);

-- 2. Tạo bảng vouchers
CREATE TABLE IF NOT EXISTS vouchers (
    voucher_id uuid PRIMARY KEY,
    start_date timestamp with time zone NOT NULL,
    end_date timestamp with time zone NOT NULL,
    voucher_code varchar(100) UNIQUE NOT NULL,
    description varchar(1000),
    discount_type varchar(50) NOT NULL,
    discount_value decimal(19,4) NOT NULL,
    min_order_value decimal(19,4) NOT NULL,
    max_uses int NOT NULL,
    used_count int NOT NULL DEFAULT 0,
    max_uses_per_user int NOT NULL DEFAULT 1,
    is_active boolean NOT NULL DEFAULT true,
    coin_cost int NOT NULL DEFAULT 0,
    target_type varchar(50) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    is_deleted boolean NOT NULL DEFAULT false
);

-- 3. Tạo bảng orders
CREATE TABLE IF NOT EXISTS orders (
    order_id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    ghn_province_id int NOT NULL,
    ghn_district_id int NOT NULL,
    ghn_ward_code varchar(50) NOT NULL,
    recipient_phone varchar(255) NOT NULL,
    receiver_name varchar(255) NOT NULL,
    ghn_order_code varchar(100),
    order_code varchar(100) UNIQUE NOT NULL,
    street_address varchar(500) NOT NULL,
    order_status varchar(50) NOT NULL,
    address_label varchar(100) NOT NULL,
    shipping_fee decimal(19,4) NOT NULL,
    order_fee decimal(19,4) NOT NULL,
    payment_method varchar(50) NOT NULL,
    payment_status varchar(50) NOT NULL,
    voucher_id uuid,
    voucher_discount_amount decimal(19,4) DEFAULT 0,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    updated_by uuid,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_province FOREIGN KEY (ghn_province_id) REFERENCES ghn_provinces(province_id),
    CONSTRAINT fk_orders_district FOREIGN KEY (ghn_district_id) REFERENCES ghn_districts(district_id),
    CONSTRAINT fk_orders_ward FOREIGN KEY (ghn_ward_code) REFERENCES ghn_wards(ward_code),
    CONSTRAINT fk_orders_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id)
);

-- 4. Tạo bảng flash_sales
CREATE TABLE IF NOT EXISTS flash_sales (
    flash_sale_id uuid PRIMARY KEY,
    book_edition_id uuid NOT NULL,
    discount_amount decimal(19,4) NOT NULL,
    flash_sale_stock int NOT NULL,
    sold_count int NOT NULL DEFAULT 0,
    is_active boolean NOT NULL DEFAULT true,
    start_date timestamp with time zone NOT NULL,
    end_date timestamp with time zone NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT fk_flash_sales_book_edition FOREIGN KEY (book_edition_id) REFERENCES book_editions(id)
);

-- 5. Tạo bảng orders_detail
CREATE TABLE IF NOT EXISTS orders_detail (
    detail_id uuid PRIMARY KEY,
    order_id uuid NOT NULL,
    book_edition_id uuid NOT NULL,
    quantity int NOT NULL,
    original_price decimal(19,4) NOT NULL,
    flash_sale_discount_amount decimal(19,4) NOT NULL DEFAULT 0,
    voucher_discount_amount decimal(19,4) NOT NULL DEFAULT 0,
    voucher_id uuid,
    flashsale_id uuid,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT fk_orders_detail_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_orders_detail_book_edition FOREIGN KEY (book_edition_id) REFERENCES book_editions(id),
    CONSTRAINT fk_orders_detail_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id),
    CONSTRAINT fk_orders_detail_flashsale FOREIGN KEY (flashsale_id) REFERENCES flash_sales(flash_sale_id)
);

-- 6. Tạo bảng payment_transactions
CREATE TABLE IF NOT EXISTS payment_transactions (
    transaction_id uuid PRIMARY KEY,
    order_code varchar(100) NOT NULL,
    transaction_code varchar(100) NOT NULL,
    amount decimal(19,4) NOT NULL,
    payment_method varchar(50) NOT NULL,
    status varchar(50) NOT NULL,
    raw_response text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT fk_payment_transactions_order_code FOREIGN KEY (order_code) REFERENCES orders(order_code)
);

-- 7. Tạo bảng order_logs
CREATE TABLE IF NOT EXISTS order_logs (
    log_id uuid PRIMARY KEY,
    order_code varchar(100) NOT NULL,
    from_status varchar(50) NOT NULL,
    to_status varchar(50) NOT NULL,
    changed_by uuid NOT NULL,
    admin_note varchar(1000),
    user_note varchar(1000),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT fk_order_logs_order_code FOREIGN KEY (order_code) REFERENCES orders(order_code)
);

-- 8. Tạo bảng user_vouchers
CREATE TABLE IF NOT EXISTS user_vouchers (
    user_voucher_id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    voucher_id uuid NOT NULL,
    status varchar(50) NOT NULL,
    order_id uuid,
    acquired_at timestamp with time zone NOT NULL,
    used_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT fk_user_vouchers_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_vouchers_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id),
    CONSTRAINT fk_user_vouchers_order FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- 9. Tạo bảng voucher_categories
CREATE TABLE IF NOT EXISTS voucher_categories (
    id uuid PRIMARY KEY,
    voucher_id uuid NOT NULL,
    category_id uuid NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT fk_voucher_categories_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id),
    CONSTRAINT fk_voucher_categories_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- 10. Tạo bảng voucher_books
CREATE TABLE IF NOT EXISTS voucher_books (
    id uuid PRIMARY KEY,
    voucher_id uuid NOT NULL,
    book_id uuid NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT fk_voucher_books_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id),
    CONSTRAINT fk_voucher_books_book FOREIGN KEY (book_id) REFERENCES books(id)
);

-- 11. Tạo bảng voucher_editions
CREATE TABLE IF NOT EXISTS voucher_editions (
    id uuid PRIMARY KEY,
    voucher_id uuid NOT NULL,
    edition_id uuid NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT fk_voucher_editions_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id),
    CONSTRAINT fk_voucher_editions_edition FOREIGN KEY (edition_id) REFERENCES book_editions(id)
);

-- 12. Tạo bảng coin_transactions
CREATE TABLE IF NOT EXISTS coin_transactions (
    transaction_id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    amount bigint NOT NULL,
    type varchar(50) NOT NULL,
    reason varchar(500),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT fk_coin_transactions_user FOREIGN KEY (user_id) REFERENCES users(id)
);
