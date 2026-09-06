-- Flyway Migration V3: Seed Genres and Languages
-- Target Database: vibecheck_movie (MySQL 8.0)
INSERT IGNORE INTO genres (name, slug) VALUES
('Action', 'action'),
('Comedy', 'comedy'),
('Drama', 'drama'),
('Sci-Fi', 'sci-fi'),
('Thriller', 'thriller'),
('Romance', 'romance'),
('Horror', 'horror'),
('Animation', 'animation'),
('Adventure', 'adventure'),
('Crime', 'crime');

INSERT IGNORE INTO languages (name, code) VALUES
('Hindi', 'hi'),
('English', 'en'),
('Tamil', 'ta'),
('Telugu', 'te'),
('Malayalam', 'ml'),
('Kannada', 'kn'),
('Marathi', 'mr'),
('Bengali', 'bn');
