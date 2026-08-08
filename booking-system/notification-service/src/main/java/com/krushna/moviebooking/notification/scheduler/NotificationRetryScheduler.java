package com.krushna.moviebooking.notification.scheduler;

import com.krushna.moviebooking.notification.channel.EmailNotificationChannel;
import com.krushna.moviebooking.notification.channel.SmsNotificationChannel;
import com.krushna.moviebooking.notification.entity.Notification;
import com.krushna.moviebooking.notification.entity.NotificationChannelType;
import com.krushna.moviebooking.notification.entity.NotificationStatus;
import com.krushna.moviebooking.notification.repository.NotificationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically retries failed notifications up to maxRetries attempts.
 * Transitions notifications exceeding maxRetries to DEAD_LETTER status.
 */
@Component
public class NotificationRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryScheduler.class);

    private final NotificationRepository notificationRepository;
    private final EmailNotificationChannel emailNotificationChannel;
    private final SmsNotificationChannel smsNotificationChannel;

    private final Counter retriedCounter;
    private final Counter deadLetterCounter;

    public NotificationRetryScheduler(
            NotificationRepository notificationRepository,
            EmailNotificationChannel emailNotificationChannel,
            SmsNotificationChannel smsNotificationChannel,
            MeterRegistry meterRegistry) {
        this.notificationRepository = notificationRepository;
        this.emailNotificationChannel = emailNotificationChannel;
        this.smsNotificationChannel = smsNotificationChannel;

        this.retriedCounter = meterRegistry.counter("notifications.retried");
        this.deadLetterCounter = meterRegistry.counter("notifications.deadletter");
    }

    @Scheduled(fixedDelayString = "${notification.retry.backoff-delay-ms:60000}")
    public void retryFailedNotifications() {
        List<Notification> failedNotifications = notificationRepository.findByStatus(NotificationStatus.FAILED);
        if (failedNotifications.isEmpty()) {
            return;
        }

        log.info("NotificationRetryScheduler found {} failed notifications for processing", failedNotifications.size());

        for (Notification notification : failedNotifications) {
            if (notification.getRetryCount() >= notification.getMaxRetries()) {
                log.warn("Notification id={} exceeded max retries ({}). Marking as DEAD_LETTER",
                        notification.getId(), notification.getMaxRetries());
                notification.setStatus(NotificationStatus.DEAD_LETTER);
                notificationRepository.save(notification);
                deadLetterCounter.increment();
                continue;
            }

            log.info("Retrying notification id={}, attempt {}/{}",
                    notification.getId(), notification.getRetryCount() + 1, notification.getMaxRetries());

            boolean delivered = false;
            if (notification.getChannelType() == NotificationChannelType.EMAIL) {
                delivered = emailNotificationChannel.send(notification);
            } else if (notification.getChannelType() == NotificationChannelType.SMS) {
                delivered = smsNotificationChannel.send(notification);
            }

            notification.setRetryCount(notification.getRetryCount() + 1);
            if (delivered) {
                notification.setStatus(NotificationStatus.SENT);
                retriedCounter.increment();
                log.info("Notification id={} successfully sent on retry", notification.getId());
            } else if (notification.getRetryCount() >= notification.getMaxRetries()) {
                notification.setStatus(NotificationStatus.DEAD_LETTER);
                deadLetterCounter.increment();
                log.warn("Notification id={} failed final retry attempt. Marked as DEAD_LETTER", notification.getId());
            }

            notificationRepository.save(notification);
        }
    }
}
