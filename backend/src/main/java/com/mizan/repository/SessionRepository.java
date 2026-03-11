package com.mizan.repository;

import com.mizan.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByRefreshTokenHashAndRevokedAtIsNull(String hash);

    @Modifying
    @Query("UPDATE Session s SET s.revokedAt = CURRENT_TIMESTAMP WHERE s.userId = :userId AND s.revokedAt IS NULL")
    int revokeAllUserSessions(UUID userId);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt < CURRENT_TIMESTAMP")
    int deleteExpiredSessions();
}
