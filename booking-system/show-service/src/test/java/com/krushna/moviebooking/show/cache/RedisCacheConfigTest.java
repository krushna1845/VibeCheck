package com.krushna.moviebooking.show.cache;

import com.krushna.moviebooking.show.config.RedisCacheConfig;
import com.krushna.moviebooking.show.dto.ShowResponse;
import com.krushna.moviebooking.show.dto.ShowSeatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisCacheConfigTest {

    @Test
    void testShowResponseSerializationAndDeserialization() {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.activateDefaultTyping(
                com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator.instance,
                com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.EVERYTHING,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        ShowSeatResponse seat = ShowSeatResponse.builder()
                .id(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .seatId(UUID.randomUUID())
                .price(new BigDecimal("350.00"))
                .status("AVAILABLE")
                .version(0L)
                .build();

        ShowResponse show = ShowResponse.builder()
                .id(UUID.randomUUID())
                .movieId(UUID.randomUUID())
                .theatreId(UUID.randomUUID())
                .screenId(UUID.randomUUID())
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(7200))
                .language("Hindi")
                .status("SCHEDULED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .seats(List.of(seat))
                .build();

        byte[] bytes = serializer.serialize(show);
        System.out.println("Serialized JSON: " + new String(bytes));
        Object deserialized = serializer.deserialize(bytes);
        assertThat(deserialized).isInstanceOf(ShowResponse.class);
    }
}
