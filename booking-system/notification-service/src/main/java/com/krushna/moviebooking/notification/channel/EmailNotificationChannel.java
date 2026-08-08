package com.krushna.moviebooking.notification.channel;

import com.krushna.moviebooking.notification.config.NotificationProperties;
import com.krushna.moviebooking.notification.entity.Notification;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Production Email Notification Channel using Spring JavaMailSender.
 * Supports HTML body and PDF attachment (e.g. ticket).
 */
@Service
public class EmailNotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public EmailNotificationChannel(JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public boolean send(Notification notification) {
        return sendWithAttachment(notification, null, null);
    }

    public boolean sendWithAttachment(Notification notification, byte[] attachmentBytes, String filename) {
        if (notification.getRecipient() == null || !notification.getRecipient().contains("@")) {
            log.warn("Invalid email recipient: {}", notification.getRecipient());
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            boolean isMultipart = attachmentBytes != null && attachmentBytes.length > 0;
            MimeMessageHelper helper = new MimeMessageHelper(message, isMultipart, "UTF-8");

            String fromAddress = properties.mail() != null && properties.mail().from() != null
                    ? properties.mail().from() : "noreply@vibecheck.com";
            String fromName = properties.mail() != null && properties.mail().fromName() != null
                    ? properties.mail().fromName() : "VibeCheck";

            helper.setFrom(fromAddress, fromName);
            helper.setTo(notification.getRecipient());
            helper.setSubject(notification.getSubject() != null ? notification.getSubject() : "VibeCheck Notification");
            helper.setText(notification.getContent() != null ? notification.getContent() : "", true);

            if (isMultipart) {
                String attachmentName = filename != null ? filename : "ticket.pdf";
                helper.addAttachment(attachmentName, new ByteArrayResource(attachmentBytes), "application/pdf");
                log.debug("Attached PDF '{}' to email for recipient={}", attachmentName, notification.getRecipient());
            }

            mailSender.send(message);
            log.info("Successfully sent email notification to recipient={}", notification.getRecipient());
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to recipient={}: {}", notification.getRecipient(), e.getMessage(), e);
            return false;
        }
    }
}
