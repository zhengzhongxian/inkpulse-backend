-- ============================================================================
-- V7__add_badge_to_books.sql
-- ============================================================================

-- 1. Alter books table to add badge_id
ALTER TABLE books ADD COLUMN badge_id UUID;

-- 2. Add foreign key constraint to badges table
ALTER TABLE books 
ADD CONSTRAINT FK_books_badges_badge_id 
FOREIGN KEY (badge_id) REFERENCES badges(id) 
ON DELETE SET NULL;

-- 3. Update existing seed data with badge_id
UPDATE books SET badge_id = '018f4e00-0000-7000-8000-000000000600' WHERE id = '018f4e00-0000-7000-8000-000000000400'; -- The Art of Clean Code -> HOT
UPDATE books SET badge_id = '018f4e00-0000-7000-8000-000000000601' WHERE id = '018f4e00-0000-7000-8000-000000000401'; -- Enterprise CQRS & Event Sourcing -> NEW
UPDATE books SET badge_id = '018f4e00-0000-7000-8000-000000000600' WHERE id = '018f4e00-0000-7000-8000-000000000402'; -- Mastering Redis Stack -> HOT
UPDATE books SET badge_id = '018f4e00-0000-7000-8000-000000000602' WHERE id = '018f4e00-0000-7000-8000-000000000403'; -- High-Performance Microservices -> RECOMMENDED
