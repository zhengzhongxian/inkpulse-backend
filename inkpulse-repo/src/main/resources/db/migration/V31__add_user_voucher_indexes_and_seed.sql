-- V31__add_user_voucher_indexes_and_seed.sql

CREATE INDEX IF NOT EXISTS idx_user_vouchers_user_status ON user_vouchers(user_id, status);
CREATE INDEX IF NOT EXISTS idx_user_vouchers_acquired ON user_vouchers(user_id, acquired_at DESC);
