package com.krushna.moviebooking.booking.model;

import com.krushna.moviebooking.common.dto.PagedResponse;
import org.springframework.data.domain.Page;

/**
 * Compatibility facade for the booking service.
 */
public final class PagedResponseCompat {
    private PagedResponseCompat() {}

    public static <T> PagedResponse<T> of(Page<T> page) {
        return PagedResponse.of(page);
    }
}
