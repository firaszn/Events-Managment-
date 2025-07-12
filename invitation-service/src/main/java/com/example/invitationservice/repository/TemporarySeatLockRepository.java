package com.example.invitationservice.repository;

import com.example.invitationservice.entity.TemporarySeatLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TemporarySeatLockRepository extends JpaRepository<TemporarySeatLock, Long> {
    
    @Query("SELECT t FROM TemporarySeatLock t WHERE t.eventId = :eventId AND t.row = :row AND t.number = :number AND t.expiryTime > :now")
    Optional<TemporarySeatLock> findActiveLock(
        @Param("eventId") Long eventId,
        @Param("row") Integer row,
        @Param("number") Integer number,
        @Param("now") LocalDateTime now
    );

    List<TemporarySeatLock> findByEventId(Long eventId);

    @Query("SELECT t FROM TemporarySeatLock t WHERE t.expiryTime <= :now")
    List<TemporarySeatLock> findExpiredLocks(@Param("now") LocalDateTime now);

    void deleteByExpiryTimeBefore(LocalDateTime time);
} 