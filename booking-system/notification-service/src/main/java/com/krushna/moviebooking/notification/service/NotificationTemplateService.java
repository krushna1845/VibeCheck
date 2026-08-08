package com.krushna.moviebooking.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Renders HTML notification content using Thymeleaf template engine.
 * Falls back to simple string substitution if Thymeleaf template rendering fails.
 */
@Service
public class NotificationTemplateService {

    private static final Logger log = LoggerFactory.getLogger(NotificationTemplateService.class);

    private final TemplateEngine templateEngine;

    public NotificationTemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(String templateKey, Map<String, Object> metadata) {
        if (metadata == null) {
            metadata = Map.of();
        }
        try {
            Context context = new Context();
            context.setVariables(metadata);
            // templateKey corresponds to template file name without .html suffix
            return templateEngine.process(templateKey, context);
        } catch (Exception e) {
            log.warn("Thymeleaf template rendering failed for key '{}': {} — using fallback text", templateKey, e.getMessage());
            return fallbackRender(templateKey, metadata);
        }
    }

    private String fallbackRender(String templateKey, Map<String, Object> metadata) {
        return switch (templateKey) {
            case "booking-confirmed" -> "Hello! Your booking %s is confirmed.".formatted(metadata.getOrDefault("bookingReference", "N/A"));
            case "booking-cancelled" -> "Hello! Your booking %s has been cancelled.".formatted(metadata.getOrDefault("bookingReference", "N/A"));
            case "booking-expired" -> "Hello! Your booking %s has expired.".formatted(metadata.getOrDefault("bookingReference", "N/A"));
            case "payment-success" -> "Payment of %s was successful for booking %s.".formatted(
                    metadata.getOrDefault("amount", "N/A"),
                    metadata.getOrDefault("bookingReference", "N/A")
            );
            case "payment-failed" -> "Payment failed for booking %s. Reason: %s".formatted(
                    metadata.getOrDefault("bookingReference", "N/A"),
                    metadata.getOrDefault("failureReason", "Unknown")
            );
            default -> "You have a new notification from VibeCheck.";
        };
    }
}
