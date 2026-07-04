-- ============================================================================
-- V6__seed_book_and_cart_data.sql
-- ============================================================================

-- 1. Publishers
INSERT INTO publishers (id, name, address, created_at, is_deleted)
VALUES
    ('018f4e00-0000-7000-8000-000000000100', 'Addison-Wesley', 'Boston, USA', NOW(), FALSE),
    ('018f4e00-0000-7000-8000-000000000101', 'O''Reilly Media', 'California, USA', NOW(), FALSE),
    ('018f4e00-0000-7000-8000-000000000102', 'Packt Publishing', 'Birmingham, UK', NOW(), FALSE)
ON CONFLICT DO NOTHING;

-- 2. Authors
INSERT INTO authors (id, name, avatar, biography, created_at, is_deleted)
VALUES
    ('018f4e00-0000-7000-8000-000000000200', 'Robert C. Martin', 'https://api.dicebear.com/7.x/pixel-art/svg?seed=unclebob', 'Uncle Bob is a software engineer and author known for promoting software design principles.', NOW(), FALSE),
    ('018f4e00-0000-7000-8000-000000000201', 'Vaughn Vernon', 'https://api.dicebear.com/7.x/pixel-art/svg?seed=vaughn', 'Vaughn Vernon is a leading expert in Domain-Driven Design (DDD) and enterprise architecture.', NOW(), FALSE),
    ('018f4e00-0000-7000-8000-000000000202', 'Salvatore Sanfilippo', 'https://api.dicebear.com/7.x/pixel-art/svg?seed=antirez', 'Salvatore Sanfilippo (antirez) is the creator of Redis, the open-source in-memory database.', NOW(), FALSE),
    ('018f4e00-0000-7000-8000-000000000203', 'Sam Newman', 'https://api.dicebear.com/7.x/pixel-art/svg?seed=samnewman', 'Sam Newman is an independent consultant specializing in cloud, continuous delivery, and microservices.', NOW(), FALSE)
ON CONFLICT DO NOTHING;

-- 3. Categories
INSERT INTO categories (id, name, slug, parent_id, created_at, is_deleted)
VALUES
    ('018f4e00-0000-7000-8000-000000000300', 'Lập trình', 'lap-trinh', NULL, NOW(), FALSE),
    ('018f4e00-0000-7000-8000-000000000301', 'Kiến trúc', 'kien-truc', NULL, NOW(), FALSE),
    ('018f4e00-0000-7000-8000-000000000302', 'Database', 'database', NULL, NOW(), FALSE)
ON CONFLICT DO NOTHING;

-- 4. Books
INSERT INTO books (id, title, introduce, description, thumbnail_url, is_active, created_at, is_deleted)
VALUES
    (
        '018f4e00-0000-7000-8000-000000000400',
        'The Art of Clean Code',
        'Kiến tạo mã nguồn chuẩn mực.',
        'Cuốn sách gối đầu giường của mọi nhà phát triển phần mềm muốn viết mã nguồn sạch đẹp, dễ bảo trì, và có cấu trúc thiết kế rõ ràng theo nguyên lý SOLID. Cung cấp các kỹ thuật refactoring mã nguồn chi tiết.',
        'https://api.dicebear.com/7.x/identicon/svg?seed=cleancode',
        TRUE,
        NOW(),
        FALSE
    ),
    (
        '018f4e00-0000-7000-8000-000000000401',
        'Enterprise CQRS & Event Sourcing',
        'Kiến trúc CQRS & Event Sourcing thực chiến.',
        'Giải thích cặn kẽ mẫu thiết kế Command Query Responsibility Segregation (CQRS) kết hợp Event Sourcing nhằm xây dựng các hệ thống phân tán chịu tải lớn, đồng bộ dữ liệu phi đối xứng một cách trơn tru.',
        'https://api.dicebear.com/7.x/identicon/svg?seed=cqrs',
        TRUE,
        NOW(),
        FALSE
    ),
    (
        '018f4e00-0000-7000-8000-000000000402',
        'Mastering Redis Stack',
        'JSON, VSS & High Scale Cache.',
        'Tập trung sâu vào các tính năng nâng cao của Redis Stack như lưu trữ tài liệu JSON động, tìm kiếm Vector toàn văn (Vector Search / VSS), phân tích dữ liệu thời gian thực và quản lý Distributed Lock.',
        'https://api.dicebear.com/7.x/identicon/svg?seed=redis',
        TRUE,
        NOW(),
        FALSE
    ),
    (
        '018f4e00-0000-7000-8000-000000000403',
        'High-Performance Microservices',
        'Thiết kế hệ thống phân tán hiệu năng cao.',
        'Hướng dẫn thiết kế kiến trúc microservices hiệu năng cao, cách kiểm soát các giao thức truyền tải bất đồng bộ (messaging queues) và giải quyết bài toán đồng bộ dữ liệu giữa các phân vùng dịch vụ độc lập.',
        'https://api.dicebear.com/7.x/identicon/svg?seed=microservices',
        TRUE,
        NOW(),
        FALSE
    )
ON CONFLICT DO NOTHING;

-- 5. Link Categories & Books
INSERT INTO categories_books (book_id, category_id)
VALUES
    ('018f4e00-0000-7000-8000-000000000400', '018f4e00-0000-7000-8000-000000000300'), -- Clean Code -> Lập trình
    ('018f4e00-0000-7000-8000-000000000401', '018f4e00-0000-7000-8000-000000000301'), -- CQRS -> Kiến trúc
    ('018f4e00-0000-7000-8000-000000000402', '018f4e00-0000-7000-8000-000000000302'), -- Redis -> Database
    ('018f4e00-0000-7000-8000-000000000403', '018f4e00-0000-7000-8000-000000000301')  -- Microservices -> Kiến trúc
ON CONFLICT DO NOTHING;

-- 6. Link Books & Authors
INSERT INTO books_authors (id, book_id, author_id, is_active, created_at, is_deleted)
VALUES
    ('018f4e00-0000-7000-8000-000000000210', '018f4e00-0000-7000-8000-000000000400', '018f4e00-0000-7000-8000-000000000200', TRUE, NOW(), FALSE),
    ('018f4e00-0000-7000-8000-000000000211', '018f4e00-0000-7000-8000-000000000401', '018f4e00-0000-7000-8000-000000000201', TRUE, NOW(), FALSE),
    ('018f4e00-0000-7000-8000-000000000212', '018f4e00-0000-7000-8000-000000000402', '018f4e00-0000-7000-8000-000000000202', TRUE, NOW(), FALSE),
    ('018f4e00-0000-7000-8000-000000000213', '018f4e00-0000-7000-8000-000000000403', '018f4e00-0000-7000-8000-000000000203', TRUE, NOW(), FALSE)
ON CONFLICT DO NOTHING;

-- 7. Book Editions
INSERT INTO book_editions (id, book_id, isbn, price, old_price, stock_quantity, edition_number, thumbnail_url, sold_count, ratings_count, rating, cover_type, page_count, publication_year, dimensions, language, publisher_id, created_at, is_deleted)
VALUES
    (
        '018f4e00-0000-7000-8000-000000000500',
        '018f4e00-0000-7000-8000-000000000400',
        '978-0132350884',
        350000.00,
        420000.00,
        150,
        1,
        'https://api.dicebear.com/7.x/identicon/svg?seed=cleancode',
        42,
        15,
        4.8,
        'Bìa Cứng',
        432,
        2024,
        '16 x 24 cm',
        'Tiếng Anh (Bản dịch tiếng Việt)',
        '018f4e00-0000-7000-8000-000000000100',
        NOW(),
        FALSE
    ),
    (
        '018f4e00-0000-7000-8000-000000000501',
        '018f4e00-0000-7000-8000-000000000401',
        '978-0134434421',
        450000.00,
        NULL,
        90,
        1,
        'https://api.dicebear.com/7.x/identicon/svg?seed=cqrs',
        18,
        6,
        4.9,
        'Bìa Cứng',
        512,
        2023,
        '16 x 24 cm',
        'Tiếng Việt',
        '018f4e00-0000-7000-8000-000000000101',
        NOW(),
        FALSE
    ),
    (
        '018f4e00-0000-7000-8000-000000000502',
        '018f4e00-0000-7000-8000-000000000402',
        '978-1801819510',
        290000.00,
        350000.00,
        80,
        1,
        'https://api.dicebear.com/7.x/identicon/svg?seed=redis',
        30,
        11,
        4.7,
        'Bìa Mềm',
        380,
        2024,
        '15 x 23 cm',
        'Tiếng Việt',
        '018f4e00-0000-7000-8000-000000000102',
        NOW(),
        FALSE
    ),
    (
        '018f4e00-0000-7000-8000-000000000503',
        '018f4e00-0000-7000-8000-000000000403',
        '978-1492040347',
        390000.00,
        NULL,
        120,
        2,
        'https://api.dicebear.com/7.x/identicon/svg?seed=microservices',
        25,
        8,
        4.6,
        'Bìa Cứng',
        460,
        2022,
        '16 x 24 cm',
        'Tiếng Việt',
        '018f4e00-0000-7000-8000-000000000101',
        NOW(),
        FALSE
    )
ON CONFLICT DO NOTHING;

-- 8. Badges
INSERT INTO badges (id, text, text_color, bg_color, created_at, is_deleted)
VALUES
    ('018f4e00-0000-7000-8000-000000000600', 'HOT', '#FFFFFF', '#F66398', NOW(), FALSE),
    ('018f4e00-0000-7000-8000-000000000601', 'NEW', '#FFFFFF', '#3B82F6', NOW(), FALSE),
    ('018f4e00-0000-7000-8000-000000000602', 'RECOMMENDED', '#FFFFFF', '#10B981', NOW(), FALSE)
ON CONFLICT DO NOTHING;

-- 9. Edition Badges
INSERT INTO editions_badges (id, edition_id, badge_id, display_order, created_at, is_deleted)
VALUES
    ('018f4e00-0000-7000-8000-000000000610', '018f4e00-0000-7000-8000-000000000500', '018f4e00-0000-7000-8000-000000000600', 0, NOW(), FALSE), -- Clean Code -> HOT
    ('018f4e00-0000-7000-8000-000000000611', '018f4e00-0000-7000-8000-000000000501', '018f4e00-0000-7000-8000-000000000601', 0, NOW(), FALSE), -- CQRS -> NEW
    ('018f4e00-0000-7000-8000-000000000612', '018f4e00-0000-7000-8000-000000000502', '018f4e00-0000-7000-8000-000000000600', 0, NOW(), FALSE), -- Redis -> HOT
    ('018f4e00-0000-7000-8000-000000000613', '018f4e00-0000-7000-8000-000000000503', '018f4e00-0000-7000-8000-000000000602', 0, NOW(), FALSE)  -- Microservices -> RECOMMENDED
ON CONFLICT DO NOTHING;

-- 10. Default Carts for Seeded Users
INSERT INTO carts (id, user_id, created_at, is_deleted)
VALUES
    ('018f4e00-0000-7000-8000-000000000700', '018f4e00-0000-7000-8000-000000000010', NOW(), FALSE), -- admin cart
    ('018f4e00-0000-7000-8000-000000000701', '018f4e00-0000-7000-8000-000000000011', NOW(), FALSE)  -- customer_demo cart
ON CONFLICT DO NOTHING;
