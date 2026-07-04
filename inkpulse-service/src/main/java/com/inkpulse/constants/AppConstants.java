package com.inkpulse.constants;

public final class AppConstants {
    private AppConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final class Pagination {
        private Pagination() {
            throw new UnsupportedOperationException("Constants class");
        }
        public static final int MIN_PAGE_SIZE = 1;
        public static final int MAX_PAGE_SIZE = 100;
        public static final int DEFAULT_PAGE_SIZE = 10;
    }

    public static final class MinioBucket {
        private MinioBucket() {
            throw new UnsupportedOperationException("Constants class");
        }
        public static final String AVATAR = "avatar";
        public static final String PDF = "pdf";
    }
}
