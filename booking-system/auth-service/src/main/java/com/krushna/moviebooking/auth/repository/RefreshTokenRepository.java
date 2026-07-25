package com.krushna.moviebooking.auth.repository;

import com.krushna.moviebooking.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserIdAndIsRevokedFalse(UUID userId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.isRevoked = true WHERE r.user.id = :userId")
    void revokeAllUserTokens(@Param("userId") UUID userId);

    void deleteByExpiresAtBefore(Instant now);
}
