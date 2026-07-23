package com.inkpulse.constants.message;

public final class AIMessageConstants {
    private AIMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String GUEST_UNAUTHORIZED = "Bạn cần đăng nhập để trò chuyện với AI.";
    public static final String MESSAGE_EMPTY = "Tin nhắn không được để trống.";
    public static final String QUOTA_EXCEEDED = "Bạn đã vượt quá giới hạn %d lượt hỏi AI trong ngày. Vui lòng quay lại vào ngày mai!";
    public static final String CHAT_SUCCESS = "Xử lý tin nhắn AI thành công";
    public static final String CHAT_SERVICE_UNAVAILABLE = "Dịch vụ AI hiện không khả dụng. Vui lòng thử lại sau!";

    public static final String CODE_GUEST_UNAUTHORIZED = "AI_GUEST_UNAUTHORIZED";
    public static final String CODE_MESSAGE_EMPTY = "AI_MESSAGE_EMPTY";
    public static final String CODE_QUOTA_EXCEEDED = "AI_QUOTA_EXCEEDED";
    public static final String CODE_CHAT_SERVICE_UNAVAILABLE = "AI_SERVICE_UNAVAILABLE";
}
