package com.krushna.moviebooking.notification.service;

import com.krushna.moviebooking.notification.channel.EmailNotificationChannel;
import com.krushna.moviebooking.notification.channel.SmsNotificationChannel;
import com.krushna.moviebooking.notification.entity.Notification;
import com.krushna.moviebooking.notification.entity.NotificationStatus;
import com.krushna.moviebooking.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateService notificationTemplateService;
    private final EmailNotificationChannel emailNotificationChannel;
    private final SmsNotificationChannel smsNotificationChannel;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationTemplateService notificationTemplateService,
            EmailNotificationChannel emailNotificationChannel,
            SmsNotificationChannel smsNotificationChannel) {
        this.notificationRepository = notificationRepository;
        this.notificationTemplateService = notificationTemplateService;
        this.emailNotificationChannel = emailNotificationChannel;
        this.smsNotificationChannel = smsNotificationChannel;
    }

    @Override
    @Transactional
    public Notification sendNotification(NotificationRequest request) {
        String renderedContent = notificationTemplateService.render(request.templateKey(), request.metadata() == null ? Map.of() : request.metadata());

        Notification notification = Notification.builder()
                .userId(request.userId())
                .recipient(request.recipient())
                .channelType(request.channelType())
                .eventType(request.eventType())
                .templateKey(request.templateKey())
                .subject(request.subject())
                .content(request.content() != null ? request.content() : renderedContent)
                .metadata(request.metadata() == null ? null : request.metadata().toString())
                .build();

        boolean delivered = switch (request.channelType()) {
            case EMAIL -> emailNotificationChannel.send(notification);
            case SMS -> smsNotificationChannel.send(notification);
        };

        notification.setStatus(delivered ? NotificationStatus.SENT : NotificationStatus.FAILED);
        notification.setRetryCount(notification.getRetryCount() + 1);

        return notificationRepository.save(notification);
    }
}
