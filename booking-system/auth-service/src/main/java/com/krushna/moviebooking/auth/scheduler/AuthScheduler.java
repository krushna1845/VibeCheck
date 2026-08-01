package com.krushna.moviebooking.auth.scheduler;

import com.krushna.moviebooking.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled jobs for auth-service housekeeping.
 * Cleans up expired refresh tokens to keep the table lean.
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class AuthScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Purge expired refresh tokens every hour.
     * This prevents unbounded growth of the refresh_tokens table.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        log.info("[AuthScheduler] Purging expired refresh tokens older than now={}", Instant.now());
        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
        log.info("[AuthScheduler] Expired refresh tokens purged successfully");
    }
}
