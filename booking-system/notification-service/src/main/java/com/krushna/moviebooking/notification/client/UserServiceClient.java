package com.krushna.moviebooking.notification.client;

import com.krushna.moviebooking.common.security.InternalAuthConstants;
import com.krushna.moviebooking.notification.config.NotificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * HTTP client for fetching user contact details from the auth/user-service.
 * Attaches internal perimeter headers so that the hardened auth-service accepts the call.
 * Falls back to a placeholder email if the user-service is unavailable.
 */
@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestTemplate restTemplate;
    private final NotificationProperties properties;
    private final String internalSecret;

    public UserServiceClient(RestTemplate restTemplate,
                             NotificationProperties properties,
                             @Value("${internal.security.secret:" + InternalAuthConstants.DEFAULT_INTERNAL_SECRET + "}") String internalSecret) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.internalSecret = internalSecret;
    }

    /**
     * Fetch user profile by userId. Returns a fallback profile on error.
     */
    public UserProfile getUserProfile(UUID userId) {
        String url = properties.userService().baseUrl() + "/api/v1/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.set(InternalAuthConstants.INTERNAL_SERVICE_HEADER, "notification-service");
        headers.set(InternalAuthConstants.INTERNAL_SECRET_HEADER, internalSecret);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            UserProfile profile = restTemplate
                    .exchange(url, HttpMethod.GET, entity, UserProfile.class)
                    .getBody();
            if (profile != null && profile.email() != null) {
                log.debug("Fetched user profile for userId={}", userId);
                return profile;
            }
        } catch (RestClientException e) {
            log.warn("Failed to fetch user profile for userId={}: {} — using fallback", userId, e.getMessage());
        }
        // Fallback: use userId-based placeholder
        return new UserProfile(userId, userId + "@placeholder.vibecheck.com", null, "User", userId.toString().substring(0, 8));
    }
}

