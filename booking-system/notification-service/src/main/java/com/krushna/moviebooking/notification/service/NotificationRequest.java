package com.krushna.moviebooking.notification.service;

import com.krushna.moviebooking.notification.entity.NotificationChannelType;
import lombok.Builder;

import java.util.Map;
import java.util.UUID;

@Builder
public record NotificationRequest(
        UUID userId,
        String recipient,
        NotificationChannelType channelType,
        String eventType,
        String templateKey,
        String subject,
        String content,
        Map<String, Object> metadata
) {}
