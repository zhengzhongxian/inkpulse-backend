package com.inkpulse.constants;

public final class QueueConstants {
    public static final String SEND_OTP_EMAIL = "send-otp-email-queue";
    public static final String SEND_CHALLENGE_EMAIL = "send-challenge-email-queue";
    public static final String SEND_DEVICE_ALERT_EMAIL = "send-device-alert-email-queue";
    public static final String SEND_FORGOT_PASSWORD_EMAIL = "send-forgot-password-email-queue";
    public static final String SYNC_AUTHOR = "sync-author-queue";
    public static final String SYNC_BOOK_EDITION = "sync-book-edition-queue";

    public static final String SYNC_PUBLISHER_NAME = "sync-publisher-name-queue";
    public static final String SYNC_CATEGORY_SLUG = "sync-category-slug-queue";

    public static final String CREATE_GHN_ORDER = "create-ghn-order-queue";
    public static final String GHN_STATUS_UPDATE = "ghn-status-update-queue";
    public static final String PAYOS_WEBHOOK = "payos-webhook-queue";

    private QueueConstants() {}
}
