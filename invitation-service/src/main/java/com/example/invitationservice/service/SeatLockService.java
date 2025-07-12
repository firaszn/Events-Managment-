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

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockService {

    private final TemporarySeatLockRepository lockRepository;
    private static final int LOCK_DURATION_MINUTES = 5;

    @Transactional
    public boolean lockSeat(Long eventId, Integer row, Integer number, String userEmail) {
        // Vérifier si la place n'est pas déjà verrouillée
        boolean isLocked = lockRepository.findActiveLock(eventId, row, number, LocalDateTime.now())
                .map(lock -> !lock.getUserEmail().equals(userEmail)) // La place est considérée libre si c'est le même utilisateur
                .orElse(false);

        if (isLocked) {
            return false;
        }

        // Créer ou mettre à jour le verrou
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
        return true;
    }

    @Transactional
    public void releaseSeat(Long eventId, Integer row, Integer number, String userEmail) {
        lockRepository.findActiveLock(eventId, row, number, LocalDateTime.now())
                .ifPresent(lock -> {
                    if (lock.getUserEmail().equals(userEmail)) {
                        lockRepository.delete(lock);
                    }
                });
    }

    @Transactional(readOnly = true)
    public List<TemporarySeatLock> getLockedSeats(Long eventId) {
        return lockRepository.findByEventId(eventId);
    }

    @Transactional
    public boolean isSeatLocked(Long eventId, Integer row, Integer number, String userEmail) {
        return lockRepository.findActiveLock(eventId, row, number, LocalDateTime.now())
                .map(lock -> !lock.getUserEmail().equals(userEmail))
                .orElse(false);
    }

    @Scheduled(fixedRate = 60000) // Exécuter toutes les minutes
    @Transactional
    public void cleanupExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        List<TemporarySeatLock> expiredLocks = lockRepository.findExpiredLocks(now);
        if (!expiredLocks.isEmpty()) {
            log.info("Nettoyage de {} verrous expirés", expiredLocks.size());
            lockRepository.deleteAll(expiredLocks);
        }
    }
} 