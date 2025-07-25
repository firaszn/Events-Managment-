package com.example.invitationservice.repository;

import com.example.invitationservice.entity.TemporarySeatLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TemporarySeatLockRepository extends JpaRepository<TemporarySeatLock, Long> {
    
    @Query("SELECT l FROM TemporarySeatLock l WHERE l.eventId = :eventId AND l.row = :row AND l.number = :number AND l.expiryTime > :now")
    Optional<TemporarySeatLock> findActiveLock(
        @Param("eventId") Long eventId,
        @Param("row") Integer row,
        @Param("number") Integer number,
        @Param("now") LocalDateTime now
    );

    @Query("SELECT l FROM TemporarySeatLock l WHERE l.expiryTime <= :now")
    List<TemporarySeatLock> findExpiredLocks(@Param("now") LocalDateTime now);

    List<TemporarySeatLock> findByEventIdAndExpiryTimeAfter(Long eventId, LocalDateTime now);

    @Modifying
    @Query("DELETE FROM TemporarySeatLock l WHERE l.expiryTime <= :now")
    void deleteExpiredLocks(@Param("now") LocalDateTime now);
} 