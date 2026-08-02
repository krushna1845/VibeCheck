package com.krushna.moviebooking.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

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
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "OK");
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
