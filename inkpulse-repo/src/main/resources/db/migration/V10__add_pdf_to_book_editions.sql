-- V10__add_pdf_to_book_editions.sql
ALTER TABLE book_editions ADD COLUMN IF NOT EXISTS file_path_pdf VARCHAR(500);
