package com.krushna.moviebooking.notification.channel;

import com.krushna.moviebooking.notification.config.NotificationProperties;
import com.krushna.moviebooking.notification.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SMS Notification Channel supporting SMS delivery.
 * In development / test mode, logs SMS payload cleanly.
 * Can be extended with Twilio / AWS SNS / MSG91 provider API integration.
 */
@Service
public class SmsNotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationChannel.class);

    private final NotificationProperties properties;

    public SmsNotificationChannel(NotificationProperties properties) {
        this.properties = properties;
    }

    public boolean send(Notification notification) {
        if (notification.getRecipient() == null || notification.getRecipient().isBlank()) {
            log.warn("Invalid SMS recipient phone number: {}", notification.getRecipient());
            return false;
        }

        boolean enabled = properties.sms() != null && properties.sms().enabled();
        if (!enabled) {
            log.info("[MOCK SMS] To: {} | Subject: {} | Message: {}",
                    notification.getRecipient(), notification.getSubject(), notification.getContent());
            return true;
        }

        try {
            // Placeholder for SMS Gateway SDK call (e.g., Twilio / AWS SNS / MSG91)
            log.info("Sending SMS via provider '{}' to recipient={}",
                    properties.sms().provider(), notification.getRecipient());
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS to recipient={}: {}", notification.getRecipient(), e.getMessage(), e);
            return false;
        }
    }
}
