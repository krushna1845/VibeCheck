package com.krushna.moviebooking.common.util;

public final class CommonUtils {
    private CommonUtils() {}

    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
