package com.krushna.moviebooking.notification.service;

import com.krushna.moviebooking.notification.channel.EmailNotificationChannel;
import com.krushna.moviebooking.notification.channel.SmsNotificationChannel;
import com.krushna.moviebooking.notification.entity.Notification;
import com.krushna.moviebooking.notification.entity.NotificationChannelType;
import com.krushna.moviebooking.notification.entity.NotificationStatus;
import com.krushna.moviebooking.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private EmailNotificationChannel emailNotificationChannel;

    @Mock
    private SmsNotificationChannel smsNotificationChannel;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldSendEmailNotificationAndPersistSentStatus() {
        NotificationRequest request = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .recipient("john@example.com")
                .channelType(NotificationChannelType.EMAIL)
                .eventType("BOOKING_CONFIRMED")
                .templateKey("booking-confirmed")
                .subject("Booking confirmed")
                .content("Your booking is confirmed")
                .metadata(Map.of("bookingReference", "BK-123"))
                .build();

        when(notificationTemplateService.render("booking-confirmed", request.metadata())).thenReturn("Your booking is confirmed");
        when(emailNotificationChannel.send(any(Notification.class))).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = notificationService.sendNotification(request);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getRecipient()).isEqualTo("john@example.com");
        verify(emailNotificationChannel).send(any(Notification.class));
    }

    @Test
    void shouldMarkNotificationAsFailedWhenChannelThrows() {
        NotificationRequest request = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .recipient("+1234567890")
                .channelType(NotificationChannelType.SMS)
                .eventType("PAYMENT_FAILED")
                .templateKey("payment-failed")
                .subject("Payment failed")
                .content("Payment failed")
                .metadata(Map.of())
                .build();

        when(notificationTemplateService.render("payment-failed", request.metadata())).thenReturn("Payment failed");
        when(smsNotificationChannel.send(any(Notification.class))).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = notificationService.sendNotification(request);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getRetryCount()).isEqualTo(1);
        verify(smsNotificationChannel).send(any(Notification.class));
    }
}
