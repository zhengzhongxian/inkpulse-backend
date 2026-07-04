-- ============================================================================
-- V8__create_ghn_and_address_tables.sql
-- ============================================================================

-- Alter users
ALTER TABLE users DROP COLUMN IF EXISTS phone_number;

-- 1. ghn_provinces
CREATE TABLE IF NOT EXISTS ghn_provinces (
    province_id   INT NOT NULL,
    province_name VARCHAR(255) NOT NULL,
    province_code VARCHAR(100),
    CONSTRAINT PK_ghn_provinces PRIMARY KEY (province_id)
);

-- 2. ghn_districts
CREATE TABLE IF NOT EXISTS ghn_districts (
    district_id   INT NOT NULL,
    province_id   INT NOT NULL,
    district_name VARCHAR(255) NOT NULL,
    district_code VARCHAR(100),
    support_type  INT,
    CONSTRAINT PK_ghn_districts PRIMARY KEY (district_id),
    CONSTRAINT FK_ghn_districts_ghn_provinces FOREIGN KEY (province_id) REFERENCES ghn_provinces(province_id) ON DELETE CASCADE
);

-- 3. ghn_wards
CREATE TABLE IF NOT EXISTS ghn_wards (
    ward_code     VARCHAR(50) NOT NULL,
    district_id   INT NOT NULL,
    ward_name     VARCHAR(255) NOT NULL,
    CONSTRAINT PK_ghn_wards PRIMARY KEY (ward_code),
    CONSTRAINT FK_ghn_wards_ghn_districts FOREIGN KEY (district_id) REFERENCES ghn_districts(district_id) ON DELETE CASCADE
);

-- 4. user_addresses
CREATE TABLE IF NOT EXISTS user_addresses (
    id              UUID NOT NULL,
    user_id         UUID NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    ghn_province_id INT NOT NULL,
    ghn_district_id INT NOT NULL,
    ghn_ward_code   VARCHAR(50) NOT NULL,
    street_address  VARCHAR(255) NOT NULL,
    address_label   VARCHAR(100),
    last_used_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_user_addresses PRIMARY KEY (id),
    CONSTRAINT FK_user_addresses_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_user_addresses_provinces FOREIGN KEY (ghn_province_id) REFERENCES ghn_provinces(province_id),
    CONSTRAINT FK_user_addresses_districts FOREIGN KEY (ghn_district_id) REFERENCES ghn_districts(district_id),
    CONSTRAINT FK_user_addresses_wards FOREIGN KEY (ghn_ward_code) REFERENCES ghn_wards(ward_code)
);

CREATE INDEX IF NOT EXISTS IDX_user_addresses_user_id ON user_addresses(user_id);
