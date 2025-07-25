package com.example.invitationservice.service;

import com.example.invitationservice.entity.TemporarySeatLock;
import com.example.invitationservice.repository.TemporarySeatLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockService {

    private final TemporarySeatLockRepository lockRepository;
    private static final int LOCK_DURATION_MINUTES = 5;

    @Transactional
    public boolean lockSeat(Long eventId, Integer row, Integer number, String userEmail) {
        try {
            log.info("Attempting to lock seat: event={}, row={}, number={}, user={}", eventId, row, number, userEmail);
            
            // Nettoyer les verrous expirés d'abord
            cleanupExpiredLocks();
            
            // Vérifier si la place n'est pas déjà verrouillée
            Optional<TemporarySeatLock> existingLock = lockRepository.findActiveLock(eventId, row, number, LocalDateTime.now());
            
            if (existingLock.isPresent()) {
                TemporarySeatLock lock = existingLock.get();
                if (lock.getUserEmail().equals(userEmail)) {
                    // Renouveler le verrou pour le même utilisateur
                    lock.setExpiryTime(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                    lockRepository.save(lock);
                    return true;
                }
                log.info("Seat is already locked by another user");
                return false;
            }

            // Créer un nouveau verrou
            LocalDateTime now = LocalDateTime.now();
            TemporarySeatLock lock = TemporarySeatLock.builder()
                    .eventId(eventId)
                    .row(row)
                    .number(number)
                    .userEmail(userEmail)
                    .lockTime(now)
                    .expiryTime(now.plusMinutes(LOCK_DURATION_MINUTES))
                    .build();

            lockRepository.save(lock);
            log.info("Successfully locked seat: event={}, row={}, number={}, user={}", eventId, row, number, userEmail);
            return true;
        } catch (Exception e) {
            log.error("Error locking seat: event={}, row={}, number={}, user={}, error={}", 
                     eventId, row, number, userEmail, e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public void releaseSeat(Long eventId, Integer row, Integer number, String userEmail) {
        try {
            log.info("Attempting to release seat: event={}, row={}, number={}, user={}", eventId, row, number, userEmail);
            lockRepository.findActiveLock(eventId, row, number, LocalDateTime.now())
                    .ifPresent(lock -> {
                        if (lock.getUserEmail().equals(userEmail)) {
                            lockRepository.delete(lock);
                            log.info("Successfully released seat");
                        } else {
                            log.warn("Cannot release seat - locked by different user");
                        }
                    });
        } catch (Exception e) {
            log.error("Error releasing seat: event={}, row={}, number={}, user={}, error={}", 
                     eventId, row, number, userEmail, e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<TemporarySeatLock> getLockedSeats(Long eventId) {
        try {
            log.info("=== DEBUT getLockedSeats pour event: {} ===", eventId);
            LocalDateTime now = LocalDateTime.now();
            log.info("Heure actuelle: {}", now);
            
            // D'abord, récupérer TOUS les verrous pour cet événement (même expirés)
            List<TemporarySeatLock> allLocks = lockRepository.findAll().stream()
                .filter(lock -> lock.getEventId().equals(eventId))
                .toList();
            log.info("Total verrous trouvés (tous) pour l'événement {}: {}", eventId, allLocks.size());
            
            for (TemporarySeatLock lock : allLocks) {
                log.info("Verrou trouvé: id={}, row={}, number={}, expiry={}, expired={}", 
                    lock.getId(), lock.getRow(), lock.getNumber(), lock.getExpiryTime(), 
                    lock.getExpiryTime().isBefore(now));
            }
            
            // Maintenant récupérer seulement les verrous actifs (non expirés)
            List<TemporarySeatLock> lockedSeats = lockRepository.findByEventIdAndExpiryTimeAfter(eventId, now);
            log.info("Verrous ACTIFS (non expirés) pour l'événement {}: {}", eventId, lockedSeats.size());
            
            for (TemporarySeatLock lock : lockedSeats) {
                log.info("Verrou actif: id={}, row={}, number={}, expiry={}", 
                    lock.getId(), lock.getRow(), lock.getNumber(), lock.getExpiryTime());
            }
            
            log.info("=== FIN getLockedSeats: {} verrous actifs retournés ===", lockedSeats.size());
            return lockedSeats;
        } catch (Exception e) {
            log.error("Error fetching locked seats for event {}: {}", eventId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Scheduled(fixedRate = 30000) // Exécuter toutes les 30 secondes
    @Transactional
    public void cleanupExpiredLocks() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<TemporarySeatLock> expiredLocks = lockRepository.findExpiredLocks(now);
            if (!expiredLocks.isEmpty()) {
                log.info("Cleaning up {} expired locks", expiredLocks.size());
                lockRepository.deleteAll(expiredLocks);
                log.info("Successfully cleaned up expired locks");
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired locks: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public boolean isSeatLocked(Long eventId, Integer row, Integer number, String userEmail) {
        try {
            return lockRepository.findActiveLock(eventId, row, number, LocalDateTime.now())
                    .map(lock -> !lock.getUserEmail().equals(userEmail))
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error checking if seat is locked: event={}, row={}, number={}, user={}, error={}", 
                     eventId, row, number, userEmail, e.getMessage(), e);
            return false;
        }
    }
} 