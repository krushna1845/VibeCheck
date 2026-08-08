package com.krushna.moviebooking.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(
        MailProperties mail,
        UserServiceProperties userService,
        RetryProperties retry,
        SmsProperties sms
) {
    public record MailProperties(String from, String fromName) {}
    public record UserServiceProperties(String baseUrl) {}
    public record RetryProperties(int maxAttempts, long backoffDelayMs) {}
    public record SmsProperties(boolean enabled, String provider) {}
}
