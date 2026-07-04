package com.inkpulse.constants;

public final class PermissionConstants {

    private PermissionConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final class Books {
        private Books() {
        }

        public static final String VIEW = "Permissions.Books.View";
        public static final String CREATE = "Permissions.Books.Create";
        public static final String EDIT = "Permissions.Books.Edit";
        public static final String DELETE = "Permissions.Books.Delete";
        public static final String INTERNAL_VIEW = "Permissions.Books.InternalView";
    }

    public static final class Categories {
        private Categories() {
        }

        public static final String VIEW = "Permissions.Categories.View";
        public static final String CREATE = "Permissions.Categories.Create";
        public static final String EDIT = "Permissions.Categories.Edit";
        public static final String DELETE = "Permissions.Categories.Delete";
        public static final String INTERNAL_VIEW = "Permissions.Categories.InternalView";
    }

    public static final class Carts {
        private Carts() {
        }

        public static final String VIEW = "Permissions.Carts.View";
        public static final String MODIFY = "Permissions.Carts.Modify";
    }

    public static final class Users {
        private Users() {
        }

        public static final String VIEW = "Permissions.Users.View";
        public static final String EDIT = "Permissions.Users.Edit";
    }

    public static final class Authors {
        private Authors() {
        }

        public static final String VIEW = "Permissions.Authors.View";
        public static final String CREATE = "Permissions.Authors.Create";
        public static final String EDIT = "Permissions.Authors.Edit";
        public static final String DELETE = "Permissions.Authors.Delete";
        public static final String INTERNAL_VIEW = "Permissions.Authors.InternalView";
    }

    public static final class Auth {
        private Auth() {
        }

        public static final String INTERNAL_LOGIN = "Permissions.Auth.InternalLogin";
    }

    public static final class Badges {
        private Badges() {
        }

        public static final String VIEW = "Permissions.Badges.View";
        public static final String CREATE = "Permissions.Badges.Create";
        public static final String EDIT = "Permissions.Badges.Edit";
        public static final String DELETE = "Permissions.Badges.Delete";
        public static final String INTERNAL_VIEW = "Permissions.Badges.InternalView";
    }

    public static final class Publishers {
        private Publishers() {
        }

        public static final String VIEW = "Permissions.Publishers.View";
        public static final String CREATE = "Permissions.Publishers.Create";
        public static final String EDIT = "Permissions.Publishers.Edit";
        public static final String DELETE = "Permissions.Publishers.Delete";
        public static final String INTERNAL_VIEW = "Permissions.Publishers.InternalView";
    }

    public static final class BookEditions {
        private BookEditions() {
        }

        public static final String ORDER = "Permissions.BookEditions.Order";
    }
}
