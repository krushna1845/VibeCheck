package com.krushna.moviebooking.notification.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationTemplateService {

    public String render(String templateKey, Map<String, Object> metadata) {
        return switch (templateKey) {
            case "booking-confirmed" -> "Hello! Your booking %s is confirmed.".formatted(metadata.getOrDefault("bookingReference", "N/A"));
            case "booking-cancelled" -> "Hello! Your booking %s has been cancelled.".formatted(metadata.getOrDefault("bookingReference", "N/A"));
            case "payment-success" -> "Payment of %s was successful for booking %s.".formatted(
                    metadata.getOrDefault("amount", "N/A"),
                    metadata.getOrDefault("bookingReference", "N/A")
            );
            case "payment-failed" -> "Payment failed for booking %s. Reason: %s".formatted(
                    metadata.getOrDefault("bookingReference", "N/A"),
                    metadata.getOrDefault("failureReason", "Unknown")
            );
            default -> "You have a new notification.";
        };
    }
}
