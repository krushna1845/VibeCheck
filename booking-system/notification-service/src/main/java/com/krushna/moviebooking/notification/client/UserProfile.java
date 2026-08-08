package com.krushna.moviebooking.notification.client;

import java.util.UUID;

public record UserProfile(
        UUID userId,
        String email,
        String phone,
        String firstName,
        String lastName
) {
    public String fullName() {
        return firstName + " " + lastName;
    }
}
