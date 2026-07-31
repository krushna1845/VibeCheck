package com.krushna.moviebooking.booking.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paginated response wrapper returned by list/search endpoints.
 *
 * <p>Wraps Spring's {@link Page} into a flat JSON structure that avoids exposing
 * internal Spring pagination implementation details to API consumers.
 *
 * @param <T> Element type contained in this page
 */
@Schema(description = "Paginated list response")
public record PagedResponse<T>(

        @Schema(description = "Page content items")
        List<T> content,

        @Schema(description = "Current zero-based page index", example = "0")
        int page,

        @Schema(description = "Number of items per page", example = "20")
        int size,

        @Schema(description = "Total number of matching records", example = "100")
        long totalElements,

        @Schema(description = "Total number of pages", example = "5")
        int totalPages,

        @Schema(description = "Whether this is the last page", example = "false")
        boolean last,

        @Schema(description = "Whether this is the first page", example = "true")
        boolean first

) {

    /**
     * Constructs a {@link PagedResponse} from a Spring {@link Page}.
     *
     * @param page Spring page result
     * @param <T>  Element type
     * @return Mapped paged response
     */
    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }
}
