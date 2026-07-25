package com.krushna.moviebooking.gateway.kafka.event;

public record GatewayEvent(String eventId, String source) {}
