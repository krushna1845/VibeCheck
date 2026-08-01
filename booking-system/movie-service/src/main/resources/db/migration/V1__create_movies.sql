-- Flyway Migration V1: Movie Service Schema
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE genres (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    slug VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE languages (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    code VARCHAR(10) NOT NULL UNIQUE
);

CREATE TABLE movies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration_minutes INT NOT NULL CHECK (duration_minutes > 0),
    release_date DATE NOT NULL,
    censor_rating VARCHAR(10) NOT NULL,
    poster_url VARCHAR(512),
    trailer_url VARCHAR(512),
    status VARCHAR(20) NOT NULL DEFAULT 'COMING_SOON',
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_movies_status ON movies(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_movies_title ON movies(title) WHERE deleted_at IS NULL;

CREATE TABLE movie_genres (
    movie_id UUID NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    genre_id INT NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (movie_id, genre_id)
);

CREATE TABLE movie_languages (
    movie_id UUID NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    language_id INT NOT NULL REFERENCES languages(id) ON DELETE CASCADE,
    PRIMARY KEY (movie_id, language_id)
);
