-- ============================================================================
-- V5__create_book_and_cart_tables.sql
-- ============================================================================

-- 1. categories
CREATE TABLE IF NOT EXISTS categories (
    id          UUID NOT NULL,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL,
    parent_id   UUID,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    version     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_categories PRIMARY KEY (id),
    CONSTRAINT FK_categories_categories_parent_id FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT UQ_categories_slug UNIQUE (slug)
);

CREATE INDEX IF NOT EXISTS IDX_categories_slug ON categories(slug);

-- 2. books
CREATE TABLE IF NOT EXISTS books (
    id            UUID NOT NULL,
    title         VARCHAR(255) NOT NULL,
    introduce     TEXT,
    description   TEXT,
    thumbnail_url VARCHAR(500),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    version       BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_books PRIMARY KEY (id)
);

-- 3. categories_books (Junction Table)
CREATE TABLE IF NOT EXISTS categories_books (
    book_id     UUID NOT NULL,
    category_id UUID NOT NULL,
    CONSTRAINT PK_categories_books PRIMARY KEY (book_id, category_id),
    CONSTRAINT FK_categories_books_books_book_id FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT FK_categories_books_categories_category_id FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- 4. authors
CREATE TABLE IF NOT EXISTS authors (
    id          UUID NOT NULL,
    name        VARCHAR(255) NOT NULL,
    avatar      VARCHAR(500),
    biography   TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    version     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_authors PRIMARY KEY (id)
);

-- 5. books_authors (Junction Table with Custom Field)
CREATE TABLE IF NOT EXISTS books_authors (
    id          UUID NOT NULL,
    book_id     UUID NOT NULL,
    author_id   UUID NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    version     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_books_authors PRIMARY KEY (id),
    CONSTRAINT FK_books_authors_books_book_id FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT FK_books_authors_authors_author_id FOREIGN KEY (author_id) REFERENCES authors(id) ON DELETE CASCADE,
    CONSTRAINT UQ_books_authors_book_author UNIQUE (book_id, author_id)
);

-- 6. publishers
CREATE TABLE IF NOT EXISTS publishers (
    id          UUID NOT NULL,
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    version     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_publishers PRIMARY KEY (id)
);

-- 7. book_editions
CREATE TABLE IF NOT EXISTS book_editions (
    id               UUID NOT NULL,
    book_id          UUID NOT NULL,
    isbn             VARCHAR(50) NOT NULL,
    price            DECIMAL(12, 2) NOT NULL,
    old_price        DECIMAL(12, 2),
    stock_quantity   INT NOT NULL DEFAULT 0,
    edition_number   INT NOT NULL DEFAULT 1,
    thumbnail_url    VARCHAR(500),
    sold_count       INT NOT NULL DEFAULT 0,
    ratings_count    INT NOT NULL DEFAULT 0,
    rating           DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    cover_type       VARCHAR(50),
    page_count       INT,
    publication_year INT,
    dimensions       VARCHAR(100),
    language         VARCHAR(50),
    publisher_id     UUID,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP,
    is_deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    version          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_book_editions PRIMARY KEY (id),
    CONSTRAINT FK_book_editions_books_book_id FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT FK_book_editions_publishers_publisher_id FOREIGN KEY (publisher_id) REFERENCES publishers(id) ON DELETE SET NULL
);

-- 8. badges
CREATE TABLE IF NOT EXISTS badges (
    id          UUID NOT NULL,
    text        VARCHAR(100) NOT NULL,
    text_color  VARCHAR(50),
    bg_color    VARCHAR(50),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    version     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_badges PRIMARY KEY (id)
);

-- 9. editions_badges
CREATE TABLE IF NOT EXISTS editions_badges (
    id            UUID NOT NULL,
    edition_id    UUID NOT NULL,
    badge_id      UUID NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    version       BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_editions_badges PRIMARY KEY (id),
    CONSTRAINT FK_editions_badges_book_editions_edition_id FOREIGN KEY (edition_id) REFERENCES book_editions(id) ON DELETE CASCADE,
    CONSTRAINT FK_editions_badges_badges_badge_id FOREIGN KEY (badge_id) REFERENCES badges(id) ON DELETE CASCADE,
    CONSTRAINT UQ_editions_badges_edition_badge UNIQUE (edition_id, badge_id)
);

-- 10. edition_images
CREATE TABLE IF NOT EXISTS edition_images (
    id            UUID NOT NULL,
    edition_id    UUID NOT NULL,
    image_url     VARCHAR(500) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    version       BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_edition_images PRIMARY KEY (id),
    CONSTRAINT FK_edition_images_book_editions_edition_id FOREIGN KEY (edition_id) REFERENCES book_editions(id) ON DELETE CASCADE
);

-- 11. carts
CREATE TABLE IF NOT EXISTS carts (
    id          UUID NOT NULL,
    user_id     UUID NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    version     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_carts PRIMARY KEY (id),
    CONSTRAINT FK_carts_users_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT UQ_carts_user_id UNIQUE (user_id)
);

-- 12. cart_items
CREATE TABLE IF NOT EXISTS cart_items (
    id         UUID NOT NULL,
    cart_id    UUID NOT NULL,
    edition_id UUID NOT NULL,
    quantity   INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version    BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_cart_items PRIMARY KEY (id),
    CONSTRAINT FK_cart_items_carts_cart_id FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT FK_cart_items_book_editions_edition_id FOREIGN KEY (edition_id) REFERENCES book_editions(id) ON DELETE CASCADE,
    CONSTRAINT UQ_cart_items_cart_edition UNIQUE (cart_id, edition_id)
);
