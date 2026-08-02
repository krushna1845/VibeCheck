package com.krushna.moviebooking.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

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
