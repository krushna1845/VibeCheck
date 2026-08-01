package com.krushna.moviebooking.movie.service;

import com.krushna.moviebooking.movie.dto.MovieRequest;
import com.krushna.moviebooking.movie.dto.MovieResponse;
import com.krushna.moviebooking.movie.dto.MovieUpdateRequest;
import com.krushna.moviebooking.movie.entity.Genre;
import com.krushna.moviebooking.movie.entity.Language;
import com.krushna.moviebooking.movie.entity.Movie;
import com.krushna.moviebooking.movie.exception.DuplicateMovieTitleException;
import com.krushna.moviebooking.movie.exception.MovieNotFoundException;
import com.krushna.moviebooking.movie.mapper.MovieMapper;
import com.krushna.moviebooking.movie.repository.GenreRepository;
import com.krushna.moviebooking.movie.repository.LanguageRepository;
import com.krushna.moviebooking.movie.repository.MovieRepository;
import com.krushna.moviebooking.movie.service.impl.MovieServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceImplTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private MovieMapper movieMapper;

    @InjectMocks
    private MovieServiceImpl movieService;

    private UUID movieId;
    private Movie movie;
    private MovieResponse movieResponse;
    private Genre actionGenre;
    private Language englishLang;

    @BeforeEach
    void setUp() {
        movieId = UUID.randomUUID();
        actionGenre = Genre.builder().id(1).name("Action").slug("action").build();
        englishLang = Language.builder().id(1).name("English").code("en").build();

        movie = Movie.builder()
                .id(movieId)
                .title("Inception")
                .description("Sci-Fi Heist")
                .durationMinutes(148)
                .releaseDate(LocalDate.of(2010, 7, 16))
                .censorRating("UA")
                .status("NOW_SHOWING")
                .genres(Set.of(actionGenre))
                .languages(Set.of(englishLang))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        movieResponse = new MovieResponse(
                movieId, "Inception", "Sci-Fi Heist", 148, LocalDate.of(2010, 7, 16),
                "UA", null, null, "NOW_SHOWING",
                Set.of(new MovieResponse.GenreSummary(1, "Action", "action")),
                Set.of(new MovieResponse.LanguageSummary(1, "English", "en")),
                Instant.now(), Instant.now()
        );
    }

    @Test
    void createMovie_Success() {
        MovieRequest request = MovieRequest.builder()
                .title("Inception")
                .description("Sci-Fi Heist")
                .durationMinutes(148)
                .releaseDate(LocalDate.of(2010, 7, 16))
                .censorRating("UA")
                .genreIds(Set.of(1))
                .languageIds(Set.of(1))
                .build();

        when(movieRepository.findByTitleContainingIgnoreCase(eq("Inception"), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(genreRepository.findById(1)).thenReturn(Optional.of(actionGenre));
        when(languageRepository.findById(1)).thenReturn(Optional.of(englishLang));
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);
        when(movieMapper.toResponse(any(Movie.class))).thenReturn(movieResponse);

        MovieResponse result = movieService.createMovie(request);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Inception");
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void createMovie_DuplicateTitle_ThrowsException() {
        MovieRequest request = MovieRequest.builder()
                .title("Inception")
                .genreIds(Set.of(1))
                .languageIds(Set.of(1))
                .build();

        when(movieRepository.findByTitleContainingIgnoreCase(eq("Inception"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movie)));

        assertThatThrownBy(() -> movieService.createMovie(request))
                .isInstanceOf(DuplicateMovieTitleException.class);

        verify(movieRepository, never()).save(any());
    }

    @Test
    void getMovieById_Success() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(movieMapper.toResponse(movie)).thenReturn(movieResponse);

        MovieResponse result = movieService.getMovieById(movieId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(movieId);
    }

    @Test
    void getMovieById_NotFound_ThrowsException() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.getMovieById(movieId))
                .isInstanceOf(MovieNotFoundException.class);
    }

    @Test
    void deleteMovie_SoftDeleteSuccess() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));

        movieService.deleteMovie(movieId);

        assertThat(movie.getDeletedAt()).isNotNull();
    }
}
