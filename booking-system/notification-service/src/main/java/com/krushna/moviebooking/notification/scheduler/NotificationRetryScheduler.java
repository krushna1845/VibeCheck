package com.krushna.moviebooking.notification.scheduler;

import com.krushna.moviebooking.notification.entity.Notification;
import com.krushna.moviebooking.notification.entity.NotificationStatus;
import com.krushna.moviebooking.notification.repository.NotificationRepository;
import com.krushna.moviebooking.notification.service.NotificationRequest;
import com.krushna.moviebooking.notification.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationRetryScheduler {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public NotificationRetryScheduler(NotificationRepository notificationRepository, NotificationService notificationService) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 60000)
    public void retryFailedNotifications() {
        List<Notification> pendingNotifications = notificationRepository.findByStatus(NotificationStatus.FAILED);
        for (Notification notification : pendingNotifications) {
            if (notification.getRetryCount() >= notification.getMaxRetries()) {
                continue;
            }

            NotificationRequest request = NotificationRequest.builder()
                    .userId(notification.getUserId())
                    .recipient(notification.getRecipient())
                    .channelType(notification.getChannelType())
                    .eventType(notification.getEventType())
                    .templateKey(notification.getTemplateKey())
                    .subject(notification.getSubject())
                    .content(notification.getContent())
                    .metadata(null)
                    .build();

            Notification retriedNotification = notificationService.sendNotification(request);
            notification.setStatus(retriedNotification.getStatus());
            notification.setRetryCount(retriedNotification.getRetryCount());
            notificationRepository.save(notification);
        }
    }
}
