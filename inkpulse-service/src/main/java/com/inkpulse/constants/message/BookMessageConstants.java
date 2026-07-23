package com.inkpulse.constants.message;

public final class BookMessageConstants {
    private BookMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final class Validate {
        private Validate() {
        }

        public static final String TITLE_EMPTY = "Tên sách không được để trống";
        public static final String TITLE_TOO_LONG = "Tên sách không được vượt quá 255 ký tự";
        public static final String INTRODUCE_TOO_LONG = "Giới thiệu sách không được vượt quá 65535 ký tự";
        public static final String DESCRIPTION_TOO_LONG = "Mô tả sách không được vượt quá 65535 ký tự";
        public static final String CATEGORY_REQUIRED = "Vui lòng chọn ít nhất một danh mục cho sách";
        public static final String AUTHOR_REQUIRED = "Vui lòng chọn ít nhất một tác giả cho sách";
        public static final String COVER_EMPTY = "Ảnh bìa không được để trống";
        public static final String IMAGE_INVALID = "Ảnh bìa không hợp lệ: ";

        public static final String ISBN_EMPTY = "ISBN không được để trống";
        public static final String ISBN_TOO_LONG = "ISBN không được vượt quá 50 ký tự";
        public static final String PRICE_EMPTY = "Giá bán không được để trống";
        public static final String PRICE_INVALID = "Giá bán phải lớn hơn 0";
        public static final String OLD_PRICE_INVALID = "Giá cũ phải lớn hơn 0";
        public static final String STOCK_INVALID = "Số lượng tồn kho không được âm";
        public static final String EDITION_NUMBER_INVALID = "Số phiên bản phải lớn hơn 0";
        public static final String COVER_TYPE_TOO_LONG = "Loại bìa không được vượt quá 50 ký tự";
        public static final String PAGE_COUNT_INVALID = "Số trang phải lớn hơn 0";
        public static final String PUBLICATION_YEAR_INVALID = "Năm xuất bản không hợp lệ";
        public static final String DIMENSIONS_TOO_LONG = "Kích thước không được vượt quá 100 ký tự";
        public static final String LANGUAGE_TOO_LONG = "Ngôn ngữ không được vượt quá 50 ký tự";
        public static final String BOOK_ID_EMPTY = "Book ID không được để trống";
    }

    public static final String TITLE_EMPTY = Validate.TITLE_EMPTY;
    public static final String COVER_EMPTY = Validate.COVER_EMPTY;
    public static final String BADGE_NOT_FOUND = "Không tìm thấy badge";
    public static final String CATEGORY_NOT_FOUND = "Một số danh mục không tồn tại";
    public static final String AUTHOR_NOT_FOUND = "Một số tác giả không tồn tại";
    public static final String READ_COVER_ERROR = "Lỗi đọc file ảnh bìa";
    public static final String UPLOAD_FAILED = "Lỗi khi upload ảnh bìa lên MinIO: ";

    public static final String CODE_TITLE_EMPTY = "BOOK_TITLE_EMPTY";
    public static final String CODE_COVER_EMPTY = "BOOK_COVER_EMPTY";
    public static final String CODE_BADGE_NOT_FOUND = "BADGE_NOT_FOUND";
    public static final String CODE_CATEGORY_NOT_FOUND = "CATEGORY_NOT_FOUND";
    public static final String CODE_AUTHOR_NOT_FOUND = "AUTHOR_NOT_FOUND";
    public static final String CODE_READ_COVER_ERROR = "FILE_READ_ERROR";
    public static final String CODE_UPLOAD_FAILED = "MINIO_UPLOAD_FAILED";

    public static final String BOOK_NOT_FOUND = "Không tìm thấy sách";
    public static final String PUBLISHER_NOT_FOUND = "Không tìm thấy nhà xuất bản";
    public static final String ISBN_EMPTY = "ISBN không được để trống";
    public static final String PRICE_INVALID = "Giá sách không hợp lệ";
    public static final String PDF_EMPTY = "File PDF không được để trống";

    public static final String CODE_BOOK_NOT_FOUND = "BOOK_NOT_FOUND";
    public static final String CODE_PUBLISHER_NOT_FOUND = "PUBLISHER_NOT_FOUND";
    public static final String CODE_ISBN_EMPTY = "BOOK_EDITION_ISBN_EMPTY";
    public static final String CODE_PRICE_INVALID = "BOOK_EDITION_PRICE_INVALID";
    public static final String CODE_PDF_EMPTY = "BOOK_EDITION_PDF_EMPTY";

    public static final String PDF_READ_ERROR = "Lỗi đọc file PDF";
    public static final String PDF_UPLOAD_FAILED = "Lỗi khi upload file PDF lên MinIO: ";
    public static final String CODE_PDF_READ_ERROR = "PDF_READ_ERROR";
    public static final String CODE_PDF_UPLOAD_FAILED = "PDF_UPLOAD_FAILED";

    // Author message constants
    public static final String AUTHOR_NAME_EMPTY = "Tên tác giả không được để trống";
    public static final String CODE_AUTHOR_NAME_EMPTY = "AUTHOR_NAME_EMPTY";
    public static final String SINGLE_AUTHOR_NOT_FOUND = "Không tìm thấy tác giả";
    public static final String CODE_SINGLE_AUTHOR_NOT_FOUND = "SINGLE_AUTHOR_NOT_FOUND";
    public static final String AVATAR_READ_ERROR = "Lỗi đọc file ảnh đại diện";
    public static final String CODE_AVATAR_READ_ERROR = "AVATAR_READ_ERROR";
    public static final String GALLERY_READ_ERROR = "Lỗi đọc file ảnh bộ sưu tập";
    public static final String CODE_GALLERY_READ_ERROR = "GALLERY_READ_ERROR";
    public static final String LIST_SUCCESS = "Lấy danh sách sách thành công!";
    public static final String INTERNAL_LIST_SUCCESS = "Lấy danh sách sách nội bộ thành công!";
    public static final String CREATE_SUCCESS = "Tạo sách mới thành công!";
    public static final String BOOK_EDITION_NOT_FOUND = "Không tìm thấy phiên bản sách";
    public static final String CODE_BOOK_EDITION_NOT_FOUND = "BOOK_EDITION_NOT_FOUND";
    public static final String DETAIL_SUCCESS = "Lấy chi tiết thành công!";
    public static final String BOOK_HAS_NO_EDITION = "Sách phải có ít nhất một phiên bản trước khi kích hoạt phát hành";
    public static final String CODE_BOOK_HAS_NO_EDITION = "BOOK_HAS_NO_EDITION";

    public static final String CREATE_EDITION_SUCCESS = "Tạo phiên bản sách mới thành công!";
    public static final String UPDATE_EDITION_SUCCESS = "Cập nhật phiên bản sách thành công!";
    public static final String DELETE_EDITION_SUCCESS = "Xóa phiên bản sách thành công!";
    public static final String PUBLISHERS_LIST_SUCCESS = "Lấy danh sách nhà xuất bản thành công!";

    public static final String CATEGORY_REQUIRED = "Vui lòng chọn ít nhất một danh mục cho sách";
    public static final String AUTHOR_REQUIRED = "Vui lòng chọn ít nhất một tác giả cho sách";
    public static final String IMAGE_VALIDATION_FAILED = "Ảnh bìa không hợp lệ: ";
    public static final String OLD_PRICE_INVALID = "Giá cũ phải lớn hơn giá hiện tại";
    public static final String STOCK_INVALID = "Số lượng tồn kho không được âm";
    public static final String EDITION_NUMBER_INVALID = "Số phiên bản phải lớn hơn 0";
    public static final String PAGE_COUNT_INVALID = "Số trang phải lớn hơn 0";
    public static final String PUBLICATION_YEAR_INVALID = "Năm xuất bản không hợp lệ";

    public static final String CODE_CATEGORY_REQUIRED = "BOOK_CATEGORY_REQUIRED";
    public static final String CODE_AUTHOR_REQUIRED = "BOOK_AUTHOR_REQUIRED";
    public static final String CODE_IMAGE_VALIDATION_FAILED = "IMAGE_VALIDATION_FAILED";
    public static final String CODE_OLD_PRICE_INVALID = "BOOK_EDITION_OLD_PRICE_INVALID";
    public static final String CODE_STOCK_INVALID = "BOOK_EDITION_STOCK_INVALID";
    public static final String CODE_EDITION_NUMBER_INVALID = "BOOK_EDITION_NUMBER_INVALID";
    public static final String CODE_PAGE_COUNT_INVALID = "BOOK_EDITION_PAGE_COUNT_INVALID";
    public static final String CODE_PUBLICATION_YEAR_INVALID = "BOOK_EDITION_PUBLICATION_YEAR_INVALID";

    // AI Vision Validation
    public static final String AI_VISION_NOT_A_BOOK = "Ảnh bìa không phải là sách. Vui lòng tải lên ảnh bìa sách hợp lệ.";
    public static final String CODE_AI_VISION_NOT_A_BOOK = "AI_VISION_NOT_A_BOOK";
    public static final String AI_VISION_RATE_LIMIT_EXCEEDED = "Bạn đã vượt quá giới hạn phân tích ảnh AI. Vui lòng thử lại sau.";
    public static final String CODE_AI_VISION_RATE_LIMIT_EXCEEDED = "AI_VISION_RATE_LIMIT_EXCEEDED";
}
