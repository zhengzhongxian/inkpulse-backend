package com.inkpulse.constants;

public final class KeyConstants {

    private KeyConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String SECTION_LOGIN_ATTEMPTS = "redis:login_attempts";
    public static final String SECTION_ACCOUNT_LOCK = "redis:account_lock";
    public static final String SECTION_OTP_LIMIT_EMAIL = "redis:otp_limit_email";
    public static final String SECTION_OTP_LIMIT_DEVICE = "redis:otp_limit_device";
    public static final String SECTION_OTP_BLOCK_EMAIL = "redis:otp_block_email";
    public static final String SECTION_OTP_BLOCK_DEVICE = "redis:otp_block_device";
    public static final String SECTION_MFA_SESSION = "redis:mfa_session";
    public static final String SECTION_REFRESH_TOKENS = "redis:refresh_tokens";
    public static final String SECTION_USER_SESSION = "redis:user_session";
    public static final String SECTION_BLACKLISTED_TOKENS = "redis:blacklisted_tokens";
    public static final String SECTION_REGISTER_OTP_SESSION = "redis:register_otp_session";
    public static final String SECTION_LOGIN_OTP_SESSION = "redis:login_otp_session";
    public static final String SECTION_FORGOT_PASSWORD_SESSION = "redis:forgot_password_session";
    public static final String SECTION_USER_PROFILE = "redis:user_profile";
    public static final String SECTION_CATEGORIES = "redis:categories";
    public static final String SECTION_CART_ITEMS = "redis:cart_items";
    public static final String SECTION_GHN_PROVINCES = "redis:ghn_provinces";
    public static final String SECTION_GHN_DISTRICTS = "redis:ghn_districts";
    public static final String SECTION_GHN_WARDS = "redis:ghn_wards";
    public static final String SECTION_AUTHOR_DETAIL = "redis:author_detail";
    public static final String SECTION_BOOK_EDITION_DETAIL = "redis:book_edition_detail";
    public static final String SECTION_PUBLISHERS = "redis:publishers";
    public static final String SECTION_BADGE_EDITIONS = "redis:badge_editions";
    public static final String SECTION_AUTHOR_EDITIONS = "redis:author_editions";
    public static final String SECTION_PUBLISHER_EDITIONS = "redis:publisher_editions";
    public static final String SECTION_CATEGORY_EDITIONS = "redis:category_editions";
    public static final String SECTION_BOOK_EDITIONS = "redis:book_editions";
    public static final String SECTION_ORDER_DETAIL = "redis:order_detail";
    public static final String SECTION_MY_ORDERS = "redis:my_orders";

    public static final String CATEGORY_LOCK_RETRY_TIMEOUT = "cache.lock.category.retry-timeout-seconds";
    public static final String CATEGORY_LOCK_RETRY_INTERVAL = "cache.lock.category.retry-interval-ms";
    public static final String GHN_LOCK_RETRY_TIMEOUT = "cache.lock.ghn.retry-timeout-seconds";
    public static final String GHN_LOCK_RETRY_INTERVAL = "cache.lock.ghn.retry-interval-ms";
    public static final String PUBLISHER_LOCK_RETRY_TIMEOUT = "cache.lock.publisher.retry-timeout-seconds";
    public static final String PUBLISHER_LOCK_RETRY_INTERVAL = "cache.lock.publisher.retry-interval-ms";

    // JWT Configs
    public static final String JWT_SECRET = "jwt.secret";
    public static final String JWT_ACCESS_TOKEN_TTL = "jwt.access-token-ttl";
    public static final String JWT_REFRESH_TOKEN_TTL = "jwt.refresh-token-ttl";

    // Cookie Configs
    public static final String COOKIE_REFRESH_TOKEN_DOMAIN = "cookie.refresh-token.domain";
    public static final String COOKIE_REFRESH_TOKEN_PATH = "cookie.refresh-token.path";
    public static final String COOKIE_REFRESH_TOKEN_SECURE = "cookie.refresh-token.secure";

    // MinIO Configs
    public static final String MINIO_ENDPOINT = "minio.endpoint";
    public static final String MINIO_ACCESS_KEY = "minio.access-key";
    public static final String MINIO_SECRET_KEY = "minio.secret-key";
    public static final String MINIO_BUCKET_NAME = "minio.bucket-name";
    public static final String MINIO_USE_SSL = "minio.use-ssl";
    public static final String MINIO_REGION = "minio.region";
    public static final String MINIO_PRESIGNED_EXPIRY_MINUTES = "minio.presigned-expiry-minutes";

    // GHN Configs
    public static final String GHN_PREFIX = "ghn";
    public static final String GHN_API_TOKEN = "ghn.api-token";
    public static final String GHN_BASE_URL = "ghn.base-url";
    public static final String GHN_SHOP_ID = "ghn.shop-id";

    // Elasticsearch & Storage URL Configs
    public static final String ELASTICSEARCH_URIS = "spring.elasticsearch.uris";
    public static final String STORAGE_PUBLIC_URL = "storage.public-url";
    public static final String STORAGE_PDF_PUBLIC_URL = "storage.pdf-public-url";

    // AES Configs
    public static final String AES_KEY = "aes.key";
    public static final String AES_IV = "aes.iv";

    // PayOS Configs
    public static final String PAYOS_PREFIX = "payos";
    public static final String PAYOS_CLIENT_ID = "payos.client-id";
    public static final String PAYOS_API_KEY = "payos.api-key";
    public static final String PAYOS_CHECKSUM_KEY = "payos.checksum-key";
    public static final String PAYOS_RETURN_URL = "payos.return-url";
    public static final String PAYOS_CANCEL_URL = "payos.cancel-url";
    public static final String PAYOS_EXPIRY_MINUTES = "payos.expiry-minutes";
}