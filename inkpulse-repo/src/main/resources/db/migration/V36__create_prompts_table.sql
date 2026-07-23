CREATE TABLE IF NOT EXISTS prompts (
    key VARCHAR(100) PRIMARY KEY,
    content TEXT NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed prompt for book vision analysis
INSERT INTO prompts (key, content, description)
VALUES (
    'vision_book_analysis',
    'You are a precise AI vision classifier. Analyze the provided image and determine:
1. Is the image primarily a book (physical or digital copy, cover, or open pages)? (Yes/No)
2. Does the image have electronic markings, digital annotations, brush strokes, or tool-based highlights/scribbles drawn over it (e.g. user markup tool, digital pen)? (Yes/No)

Provide the response strictly in JSON format with the following keys:
- "is_book": boolean,
- "is_electronically_marked": boolean,
- "confidence": float (0.0 to 1.0),
- "reason": string (short explanation in Vietnamese)',
    'Prompt template for analyzing image for book detection and electronic markings'
)
ON CONFLICT (key) DO NOTHING;
