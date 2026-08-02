package com.krushna.moviebooking.booking.model;

import com.krushna.moviebooking.common.dto.ApiResponse;

/**
 * Compatibility facade for the booking service.
 */
public final class ApiResponseCompat {
    private ApiResponseCompat() {}

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.success(data, message);
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.success(data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.error(message);
    }
}
