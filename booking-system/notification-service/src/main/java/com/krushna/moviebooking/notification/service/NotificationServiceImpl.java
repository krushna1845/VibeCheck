package com.krushna.moviebooking.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krushna.moviebooking.notification.channel.EmailNotificationChannel;
import com.krushna.moviebooking.notification.channel.SmsNotificationChannel;
import com.krushna.moviebooking.notification.client.UserProfile;
import com.krushna.moviebooking.notification.client.UserServiceClient;
import com.krushna.moviebooking.notification.entity.Notification;
import com.krushna.moviebooking.notification.entity.NotificationChannelType;
import com.krushna.moviebooking.notification.entity.NotificationStatus;
import com.krushna.moviebooking.notification.pdf.TicketPdfGenerator;
import com.krushna.moviebooking.notification.repository.NotificationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateService notificationTemplateService;
    private final EmailNotificationChannel emailNotificationChannel;
    private final SmsNotificationChannel smsNotificationChannel;
    private final UserServiceClient userServiceClient;
    private final TicketPdfGenerator ticketPdfGenerator;
    private final ObjectMapper objectMapper;

    private final Counter sentCounter;
    private final Counter failedCounter;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationTemplateService notificationTemplateService,
            EmailNotificationChannel emailNotificationChannel,
            SmsNotificationChannel smsNotificationChannel,
            UserServiceClient userServiceClient,
            TicketPdfGenerator ticketPdfGenerator,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.notificationRepository = notificationRepository;
        this.notificationTemplateService = notificationTemplateService;
        this.emailNotificationChannel = emailNotificationChannel;
        this.smsNotificationChannel = smsNotificationChannel;
        this.userServiceClient = userServiceClient;
        this.ticketPdfGenerator = ticketPdfGenerator;
        this.objectMapper = objectMapper;

        this.sentCounter = meterRegistry.counter("notifications.sent");
        this.failedCounter = meterRegistry.counter("notifications.failed");
    }

    @Override
    @Transactional
    public Notification sendNotification(NotificationRequest request) {
        log.info("Processing notification request for userId={}, eventType={}, channel={}",
                request.userId(), request.eventType(), request.channelType());

        // 1. Resolve recipient email/phone from UserServiceClient if not provided or placeholder
        String recipient = request.recipient();
        String userName = "Valued Customer";
        if (request.userId() != null) {
            UserProfile profile = userServiceClient.getUserProfile(request.userId());
            if (profile != null) {
                userName = profile.fullName();
                if (request.channelType() == NotificationChannelType.EMAIL && profile.email() != null) {
                    recipient = profile.email();
                } else if (request.channelType() == NotificationChannelType.SMS && profile.phone() != null) {
                    recipient = profile.phone();
                }
            }
        }

        // 2. Enrich metadata with resolved userName
        Map<String, Object> metadataMap = request.metadata() != null ? new HashMap<>(request.metadata()) : new HashMap<>();
        metadataMap.putIfAbsent("userName", userName);

        // 3. Render template
        String renderedContent = request.content() != null ? request.content()
                : notificationTemplateService.render(request.templateKey(), metadataMap);

        String metadataJson = null;
        try {
            metadataJson = objectMapper.writeValueAsString(metadataMap);
        } catch (Exception e) {
            log.warn("Failed to serialize notification metadata: {}", e.getMessage());
        }

        // 4. Build and save initial Notification entity
        Notification notification = Notification.builder()
                .userId(request.userId())
                .recipient(recipient)
                .channelType(request.channelType())
                .eventType(request.eventType())
                .templateKey(request.templateKey())
                .subject(request.subject())
                .content(renderedContent)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .metadata(metadataJson)
                .build();

        notification = notificationRepository.save(notification);

        // 5. Dispatch notification based on channel
        boolean delivered = false;
        if (request.channelType() == NotificationChannelType.EMAIL) {
            if ("BOOKING_CONFIRMED".equalsIgnoreCase(request.eventType())) {
                // Generate PDF ticket & QR Code
                try {
                    byte[] pdfBytes = ticketPdfGenerator.generate(metadataMap);
                    String filename = "Ticket_" + metadataMap.getOrDefault("bookingReference", "VibeCheck") + ".pdf";
                    delivered = emailNotificationChannel.sendWithAttachment(notification, pdfBytes, filename);
                } catch (Exception e) {
                    log.error("Failed to attach ticket PDF, falling back to standard email: {}", e.getMessage());
                    delivered = emailNotificationChannel.send(notification);
                }
            } else {
                delivered = emailNotificationChannel.send(notification);
            }
        } else if (request.channelType() == NotificationChannelType.SMS) {
            delivered = smsNotificationChannel.send(notification);
        }

        // 6. Update status and metrics
        if (delivered) {
            notification.setStatus(NotificationStatus.SENT);
            sentCounter.increment();
            log.info("Notification id={} successfully sent to {}", notification.getId(), recipient);
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            failedCounter.increment();
            log.warn("Notification id={} failed to send to {}", notification.getId(), recipient);
        }
        notification.setRetryCount(notification.getRetryCount() + 1);

        return notificationRepository.save(notification);
    }
}
