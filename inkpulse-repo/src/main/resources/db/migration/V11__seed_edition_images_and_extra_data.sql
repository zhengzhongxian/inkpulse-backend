-- ============================================================================
-- V11__seed_edition_images_and_extra_data.sql
-- ============================================================================

-- 1. Seed extra book editions for Clean Code (book_id: 018f4e00-0000-7000-8000-000000000400)
-- Cheaper Softcover Edition
INSERT INTO book_editions (id, book_id, isbn, price, old_price, stock_quantity, edition_number, thumbnail_url, sold_count, ratings_count, rating, cover_type, page_count, publication_year, dimensions, language, publisher_id, created_at, is_deleted)
VALUES (
    '018f4e00-0000-7000-8000-000000000510',
    '018f4e00-0000-7000-8000-000000000400',
    '978-0132350885',
    280000.00,
    350000.00,
    120,
    2,
    'https://api.dicebear.com/7.x/identicon/svg?seed=cleancodesoft',
    10,
    3,
    4.5,
    'Bìa Mềm',
    432,
    2024,
    '16 x 24 cm',
    'Tiếng Anh (Bản dịch tiếng Việt)',
    '018f4e00-0000-7000-8000-000000000100',
    NOW(),
    FALSE
) ON CONFLICT DO NOTHING;

-- Special Collector's Edition
INSERT INTO book_editions (id, book_id, isbn, price, old_price, stock_quantity, edition_number, thumbnail_url, sold_count, ratings_count, rating, cover_type, page_count, publication_year, dimensions, language, publisher_id, created_at, is_deleted)
VALUES (
    '018f4e00-0000-7000-8000-000000000511',
    '018f4e00-0000-7000-8000-000000000400',
    '978-0132350886',
    600000.00,
    NULL,
    30,
    3,
    'https://api.dicebear.com/7.x/identicon/svg?seed=cleancodespecial',
    2,
    1,
    5.0,
    'Bản Đặc Biệt',
    450,
    2025,
    '17 x 25 cm',
    'Tiếng Anh',
    '018f4e00-0000-7000-8000-000000000100',
    NOW(),
    FALSE
) ON CONFLICT DO NOTHING;

-- 2. Seed Edition Gallery Images for Hardcover Edition (edition_id: 018f4e00-0000-7000-8000-000000000500)
INSERT INTO edition_images (id, edition_id, image_url, display_order, created_at, is_deleted)
VALUES 
    ('018f4e00-0000-7000-8000-000000000900', '018f4e00-0000-7000-8000-000000000500', 'editions/images/cleancode_1.jpg', 1, NOW(), FALSE),
    ('018f4e00-0000-7000-8000-000000000901', '018f4e00-0000-7000-8000-000000000500', 'editions/images/cleancode_2.jpg', 2, NOW(), FALSE)
ON CONFLICT DO NOTHING;
