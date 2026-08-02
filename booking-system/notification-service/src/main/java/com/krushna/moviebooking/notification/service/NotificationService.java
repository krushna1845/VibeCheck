package com.krushna.moviebooking.notification.service;

import com.krushna.moviebooking.notification.entity.Notification;

public interface NotificationService {
    Notification sendNotification(NotificationRequest request);
}
