package com.krushna.moviebooking.notification.channel;

import com.krushna.moviebooking.notification.entity.Notification;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationChannel {

    public boolean send(Notification notification) {
        return notification.getRecipient() != null && notification.getRecipient().contains("@");
    }
}
