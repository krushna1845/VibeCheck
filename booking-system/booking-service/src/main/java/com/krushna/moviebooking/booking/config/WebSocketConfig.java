package com.krushna.moviebooking.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Spring WebSocket + STOMP configuration with SockJS fallback.
 *
 * <p><b>WebSocket Endpoint:</b> {@code /ws-seat-availability}
 * <br><b>SockJS fallback:</b> Enabled for HTTP-polling clients.
 * <br><b>Application destination prefix:</b> {@code /app}
 * <br><b>Topic broker prefix:</b> {@code /topic}
 *
 * <p><b>Client subscription topic:</b> {@code /topic/show/{showId}}
 * <br>Clients subscribe to this topic to receive real-time seat availability events
 * for a specific show. Events are published from the server side via
 * {@link WebSocketSeatAvailabilityPublisher}.
 *
 * <p>Events are broadcast automatically by the server; clients do NOT need to
 * send STOMP messages to trigger updates — subscriptions are receive-only.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Register the STOMP/WebSocket handshake endpoint with SockJS fallback.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws-seat-availability")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Configure the in-memory message broker.
     *
     * <ul>
     *   <li>{@code /topic} prefix: server-to-client broadcast topics</li>
     *   <li>{@code /app} prefix: client-to-server message routing (reserved for future commands)</li>
     * </ul>
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
