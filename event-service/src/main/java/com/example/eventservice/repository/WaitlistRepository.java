package com.example.eventservice.repository;

import com.example.eventservice.entity.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {
    
    /**
     * Trouve les entrées en attente pour un événement, triées par position
     */
    @Query("SELECT w FROM WaitlistEntry w WHERE w.eventId = :eventId AND w.status = 'WAITING' ORDER BY w.position ASC")
    List<WaitlistEntry> findWaitingByEventId(@Param("eventId") Long eventId);
    
    /**
     * Trouve une entrée spécifique pour un utilisateur et un événement
     */
    Optional<WaitlistEntry> findByEventIdAndUserEmail(Long eventId, String userEmail);
    
    /**
     * Compte le nombre de personnes en attente pour un événement
     */
    @Query("SELECT COUNT(w) FROM WaitlistEntry w WHERE w.eventId = :eventId AND w.status = 'WAITING'")
    long countWaitingByEventId(@Param("eventId") Long eventId);
    
    /**
     * Trouve la prochaine position disponible pour un événement
     */
    @Query("SELECT COALESCE(MAX(w.position), 0) + 1 FROM WaitlistEntry w WHERE w.eventId = :eventId")
    int getNextPosition(@Param("eventId") Long eventId);
    
    /**
     * Trouve les entrées notifiées qui ont expiré
     */
    @Query("SELECT w FROM WaitlistEntry w WHERE w.status = 'NOTIFIED' AND w.expiresAt < :now")
    List<WaitlistEntry> findExpiredNotifications(@Param("now") LocalDateTime now);
    
    /**
     * Trouve les N premières personnes en attente pour un événement
     */
    @Query("SELECT w FROM WaitlistEntry w WHERE w.eventId = :eventId AND w.status = 'WAITING' ORDER BY w.position ASC")
    List<WaitlistEntry> findTopWaitingByEventId(@Param("eventId") Long eventId, @Param("limit") int limit);
    
    /**
     * Met à jour les positions après suppression d'une entrée
     */
    @Query("UPDATE WaitlistEntry w SET w.position = w.position - 1 WHERE w.eventId = :eventId AND w.position > :deletedPosition")
    void updatePositionsAfterDeletion(@Param("eventId") Long eventId, @Param("deletedPosition") int deletedPosition);
} 