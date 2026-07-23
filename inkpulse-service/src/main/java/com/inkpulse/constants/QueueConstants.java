package com.inkpulse.constants;

public final class QueueConstants {
    public static final String SEND_OTP_EMAIL = "send-otp-email-queue";
    public static final String SEND_CHALLENGE_EMAIL = "send-challenge-email-queue";
    public static final String SEND_DEVICE_ALERT_EMAIL = "send-device-alert-email-queue";
    public static final String SEND_FORGOT_PASSWORD_EMAIL = "send-forgot-password-email-queue";
    public static final String SYNC_AUTHOR = "sync-author-queue";
    public static final String SYNC_BOOK_EDITION = "sync-book-edition-queue";
    public static final String SYNC_BOOK_EDITION_PARTIAL = "sync-book-edition-partial-queue";

    public static final String SYNC_PUBLISHER_NAME = "sync-publisher-name-queue";
    public static final String SYNC_CATEGORY_SLUG = "sync-category-slug-queue";
    public static final String SYNC_FLASHSALE_TO_ELS = "sync-flashsale-to-els-queue";

    public static final String CREATE_GHN_ORDER = "create-ghn-order-queue";
    public static final String GHN_STATUS_UPDATE = "ghn-status-update-queue";
    public static final String PAYOS_WEBHOOK = "payos-webhook-queue";
    public static final String CANCEL_GHN_ORDER = "cancel-ghn-order-queue";
    public static final String RETURN_GHN_ORDER = "return-ghn-order-queue";

    private QueueConstants() {}
}
