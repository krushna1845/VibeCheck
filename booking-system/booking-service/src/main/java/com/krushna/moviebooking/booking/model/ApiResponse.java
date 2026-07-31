package com.krushna.moviebooking.booking.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

/**
 * Generic API response envelope for single-resource responses.
 *
 * <p>Wraps any payload with metadata fields ({@code success}, {@code message}, {@code timestamp})
 * so all non-paginated endpoints return a consistent JSON structure.
 *
 * @param <T> Payload type
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public record ApiResponse<T>(

        @Schema(description = "Indicates whether the request was processed successfully", example = "true")
        boolean success,

        @Schema(description = "Human-readable message describing the result", example = "Booking created successfully")
        String message,

        @Schema(description = "Response payload")
        T data,

        @Schema(description = "Server-side UTC timestamp", example = "2026-08-01T00:00:00Z")
        Instant timestamp

) {

    /**
     * Factory method – successful response with data and message.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Factory method – successful response with data only.
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "OK");
    }

    /**
     * Factory method – error response with message only (no data).
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
