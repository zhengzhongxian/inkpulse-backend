-- ============================================================================
-- V16__rename_description_add_badge_shape.sql
-- ============================================================================

-- 1. Rename description -> short_description on books table
ALTER TABLE books RENAME COLUMN description TO short_description;

-- 2. Add shape column to badges table
ALTER TABLE badges ADD COLUMN shape VARCHAR(50) DEFAULT 'pill';