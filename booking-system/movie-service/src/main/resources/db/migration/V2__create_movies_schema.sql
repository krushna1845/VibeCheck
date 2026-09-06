-- Flyway Migration V2: Movie Service (Movie Catalog Aggregate)
-- Target Database: vibecheck_movie (MySQL 8.0)

-- 1. Table: movies
CREATE TABLE movies (
    id BINARY(16) NOT NULL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    duration_minutes INT NOT NULL,
    release_date DATE NOT NULL,
    censor_rating VARCHAR(10) NOT NULL,
    poster_url VARCHAR(512) NULL,
    trailer_url VARCHAR(512) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMING_SOON',
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_movies_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_movies_censor CHECK (censor_rating IN ('U', 'UA', 'A', 'S')),
    CONSTRAINT chk_movies_status CHECK (status IN ('COMING_SOON', 'NOW_SHOWING', 'ENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes on movies table
CREATE INDEX idx_movies_status_release ON movies(status, release_date);
CREATE INDEX idx_movies_release_date ON movies(release_date);

-- 2. Table: genres
CREATE TABLE genres (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(50) NOT NULL,
    CONSTRAINT uk_genres_name UNIQUE (name),
    CONSTRAINT uk_genres_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_genres_slug ON genres(slug);

-- 3. Table: movie_genres (Junction Table)
CREATE TABLE movie_genres (
    movie_id BINARY(16) NOT NULL,
    genre_id INT NOT NULL,
    PRIMARY KEY (movie_id, genre_id),
    CONSTRAINT fk_movie_genres_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    CONSTRAINT fk_movie_genres_genre FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_movie_genres_movie_id ON movie_genres(movie_id);
CREATE INDEX idx_movie_genres_genre_id ON movie_genres(genre_id);

-- 4. Table: languages
CREATE TABLE languages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(10) NOT NULL,
    CONSTRAINT uk_languages_name UNIQUE (name),
    CONSTRAINT uk_languages_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_languages_code ON languages(code);

-- 5. Table: movie_languages (Junction Table)
CREATE TABLE movie_languages (
    movie_id BINARY(16) NOT NULL,
    language_id INT NOT NULL,
    PRIMARY KEY (movie_id, language_id),
    CONSTRAINT fk_movie_languages_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    CONSTRAINT fk_movie_languages_language FOREIGN KEY (language_id) REFERENCES languages(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_movie_languages_movie_id ON movie_languages(movie_id);
CREATE INDEX idx_movie_languages_language_id ON movie_languages(language_id);
