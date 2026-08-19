-- Flyway Migration V2: Seed Genres and Languages
INSERT INTO genres (name, slug) VALUES
('Action', 'action'),
('Comedy', 'comedy'),
('Drama', 'drama'),
('Sci-Fi', 'sci-fi'),
('Thriller', 'thriller'),
('Romance', 'romance'),
('Horror', 'horror'),
('Animation', 'animation'),
('Adventure', 'adventure'),
('Crime', 'crime')
ON CONFLICT (name) DO NOTHING;

INSERT INTO languages (name, code) VALUES
('Hindi', 'hi'),
('English', 'en'),
('Tamil', 'ta'),
('Telugu', 'te'),
('Malayalam', 'ml'),
('Kannada', 'kn'),
('Marathi', 'mr'),
('Bengali', 'bn')
ON CONFLICT (name) DO NOTHING;
