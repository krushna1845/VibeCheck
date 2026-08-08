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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private EmailNotificationChannel emailNotificationChannel;

    @Mock
    private SmsNotificationChannel smsNotificationChannel;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private TicketPdfGenerator ticketPdfGenerator;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        notificationService = new NotificationServiceImpl(
                notificationRepository,
                notificationTemplateService,
                emailNotificationChannel,
                smsNotificationChannel,
                userServiceClient,
                ticketPdfGenerator,
                objectMapper,
                meterRegistry
        );
    }

    @Test
    void shouldSendEmailNotificationAndPersistSentStatus() {
        UUID userId = UUID.randomUUID();
        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .recipient("john@example.com")
                .channelType(NotificationChannelType.EMAIL)
                .eventType("PAYMENT_SUCCESS")
                .templateKey("payment-success")
                .subject("Payment Successful")
                .content("Payment successful")
                .metadata(Map.of("bookingReference", "BK-123"))
                .build();

        when(userServiceClient.getUserProfile(userId))
                .thenReturn(new UserProfile(userId, "john@example.com", null, "John", "Doe"));
        when(emailNotificationChannel.send(any(Notification.class))).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = notificationService.sendNotification(request);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getRecipient()).isEqualTo("john@example.com");
        verify(emailNotificationChannel).send(any(Notification.class));
    }

    @Test
    void shouldSendEmailWithPdfAttachmentWhenBookingConfirmed() {
        UUID userId = UUID.randomUUID();
        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .recipient("john@example.com")
                .channelType(NotificationChannelType.EMAIL)
                .eventType("BOOKING_CONFIRMED")
                .templateKey("booking-confirmed")
                .subject("Booking Confirmed")
                .metadata(Map.of("bookingReference", "BK-999"))
                .build();

        byte[] fakePdf = "PDF_CONTENT".getBytes();
        when(userServiceClient.getUserProfile(userId))
                .thenReturn(new UserProfile(userId, "john@example.com", null, "John", "Doe"));
        when(ticketPdfGenerator.generate(any())).thenReturn(fakePdf);
        when(emailNotificationChannel.sendWithAttachment(any(Notification.class), eq(fakePdf), anyString())).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = notificationService.sendNotification(request);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(ticketPdfGenerator).generate(any());
        verify(emailNotificationChannel).sendWithAttachment(any(Notification.class), eq(fakePdf), anyString());
    }

    @Test
    void shouldMarkNotificationAsFailedWhenChannelFails() {
        UUID userId = UUID.randomUUID();
        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .recipient("+1234567890")
                .channelType(NotificationChannelType.SMS)
                .eventType("PAYMENT_FAILED")
                .templateKey("payment-failed")
                .subject("Payment Failed")
                .content("Payment failed")
                .metadata(Map.of())
                .build();

        when(userServiceClient.getUserProfile(userId))
                .thenReturn(new UserProfile(userId, null, "+1234567890", "Jane", "Doe"));
        when(smsNotificationChannel.send(any(Notification.class))).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = notificationService.sendNotification(request);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getRetryCount()).isEqualTo(1);
        verify(smsNotificationChannel).send(any(Notification.class));
    }
}
