package com.example.invitationservice.controller;

import com.example.invitationservice.entity.InvitationEntity;
import com.example.invitationservice.entity.SeatInfo;
import com.example.invitationservice.entity.TemporarySeatLock;
import com.example.invitationservice.model.InvitationRequest;
import com.example.invitationservice.model.InvitationResponse;
import com.example.invitationservice.repository.InvitationRepository;
import com.example.invitationservice.service.InvitationService;
import com.example.invitationservice.service.SeatLockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import com.example.invitationservice.exception.InvitationException;

@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
@Slf4j
public class

InvitationController {

    private final InvitationService invitationService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SeatLockService seatLockService;
    private final InvitationRepository invitationRepository;
    
    @Value("${kafka.topics.invitation-responded}")
    private String invitationRespondedTopic;

    @GetMapping
    public ResponseEntity<List<InvitationResponse>> getAllInvitations() {
        log.info("Fetching all invitations");
        List<InvitationEntity> invitations = invitationService.getAllInvitations();
        List<InvitationResponse> responses = invitations.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<InvitationResponse> createInvitation(@Valid @RequestBody InvitationRequest request) {
        log.info("Received invitation request for event {} from user {}", request.getEventId(), request.getUserEmail());
        InvitationEntity invitation = invitationService.createInvitation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(invitation));
    }

    @GetMapping("/check/{eventId}/{userEmail}")
    public ResponseEntity<Boolean> isUserRegisteredForEvent(
            @PathVariable Long eventId,
            @PathVariable String userEmail) {
        log.info("Checking registration for event {} and user {}", eventId, userEmail);
        boolean isRegistered = invitationService.isUserRegisteredForEvent(eventId, userEmail);
        return ResponseEntity.ok(isRegistered);
    }

    @GetMapping("/check-pending/{eventId}/{userEmail}")
    public ResponseEntity<Boolean> hasUserPendingInvitation(
            @PathVariable Long eventId,
            @PathVariable String userEmail) {
        log.info("Checking pending invitation for event {} and user {}", eventId, userEmail);
        boolean hasPending = invitationService.hasUserPendingInvitation(eventId, userEmail);
        return ResponseEntity.ok(hasPending);
    }

    @GetMapping("/event/{eventId}/occupied-seats")
    public ResponseEntity<List<SeatInfo>> getOccupiedSeats(@PathVariable String eventId) {
        try {
            log.info("=== DEBUT getOccupiedSeats pour event ID: {} ===", eventId);
            
            if (eventId == null || eventId.trim().isEmpty()) {
                log.error("Event ID is null or empty");
                return ResponseEntity.badRequest().build();
            }
            
            Long eventIdLong;
            try {
                eventIdLong = Long.parseLong(eventId);
            } catch (NumberFormatException e) {
                log.error("Invalid event ID format: {}", eventId);
                return ResponseEntity.badRequest().build();
            }
            
            // Créer un nouveau HashSet mutable
            Set<SeatInfo> occupiedSeats = new HashSet<>();
            
            try {
                // Ajouter les places réservées
                log.info("Récupération des places réservées pour l'événement {}", eventIdLong);
                List<SeatInfo> reservedSeats = invitationService.getOccupiedSeatsForEvent(eventIdLong);
                log.info("Places réservées trouvées: {} pour l'événement {}", reservedSeats != null ? reservedSeats.size() : 0, eventIdLong);
                
                if (reservedSeats != null && !reservedSeats.isEmpty()) {
                    // Créer une nouvelle liste mutable pour éviter UnsupportedOperationException
                    List<SeatInfo> mutableReservedSeats = new ArrayList<>();
                    for (SeatInfo seat : reservedSeats) {
                        if (seat != null) {
                            log.debug("Place réservée ajoutée: row={}, number={}", seat.getRow(), seat.getNumber());
                            mutableReservedSeats.add(seat);
                        }
                    }
                    occupiedSeats.addAll(mutableReservedSeats);
                    log.info("Total places réservées ajoutées: {}", mutableReservedSeats.size());
                } else {
                    log.info("Aucune place réservée trouvée pour l'événement {}", eventIdLong);
                }
                
                // Ajouter les places temporairement verrouillées
                log.info("Récupération des places verrouillées pour l'événement {}", eventIdLong);
                List<TemporarySeatLock> lockedSeats = seatLockService.getLockedSeats(eventIdLong);
                log.info("Places verrouillées trouvées: {} pour l'événement {}", lockedSeats != null ? lockedSeats.size() : 0, eventIdLong);
                
                if (lockedSeats != null && !lockedSeats.isEmpty()) {
                    log.info("Traitement de {} places verrouillées", lockedSeats.size());
                    for (TemporarySeatLock lock : lockedSeats) {
                        if (lock != null && lock.getRow() != null && lock.getNumber() != null) {
                        SeatInfo seatInfo = SeatInfo.builder()
                            .row(lock.getRow())
                            .number(lock.getNumber())
                            .build();
                            log.info("Place verrouillée ajoutée: row={}, number={}, expiry={}", 
                                lock.getRow(), lock.getNumber(), lock.getExpiryTime());
                        occupiedSeats.add(seatInfo);
                        } else {
                            log.warn("Place verrouillée invalide ignorée: {}", lock);
                        }
                    }
                } else {
                    log.info("Aucune place verrouillée active trouvée pour l'événement {}", eventIdLong);
                }
            } catch (Exception e) {
                log.error("Error processing seats: {}", e.getMessage(), e);
            }
            
            List<SeatInfo> result = new ArrayList<>(occupiedSeats);
            log.info("=== RESULTAT FINAL: {} places occupées total pour l'événement {} ===", result.size(), eventIdLong);
            
            // Log détaillé du résultat
            if (result.isEmpty()) {
                log.warn("ATTENTION: Liste vide retournée pour l'événement {} alors qu'on pourrait s'attendre à des données", eventIdLong);
            } else {
                for (SeatInfo seat : result) {
                    log.info("Place occupée finale: row={}, number={}", seat.getRow(), seat.getNumber());
                }
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Unexpected error getting occupied seats for event {}: {}", eventId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/event/{eventId}/lock-seat")
    public ResponseEntity<Void> lockSeat(
            @PathVariable String eventId,
            @RequestBody SeatInfo seatInfo,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String userEmail = jwt.getClaimAsString("email");
            boolean locked = seatLockService.lockSeat(
                Long.parseLong(eventId),
                seatInfo.getRow(),
                seatInfo.getNumber(),
                userEmail
            );

            if (locked) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        } catch (Exception e) {
            log.error("Error locking seat: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/event/{eventId}/release-seat")
    public ResponseEntity<Void> releaseSeat(
            @PathVariable String eventId,
            @RequestBody SeatInfo seatInfo,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String userEmail = jwt.getClaimAsString("email");
            seatLockService.releaseSeat(
                Long.parseLong(eventId),
                seatInfo.getRow(),
                seatInfo.getNumber(),
                userEmail
            );
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error releasing seat: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{invitationId}/confirm")
    public ResponseEntity<InvitationResponse> confirmInvitation(@PathVariable Long invitationId) {
        log.info("Confirming invitation with ID: {}", invitationId);
        InvitationEntity invitation = invitationService.confirmInvitation(invitationId);
        return ResponseEntity.ok(toResponse(invitation));
    }

    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> deleteInvitation(@PathVariable Long invitationId) {
        log.info("Deleting invitation with ID: {}", invitationId);
        try {
            invitationService.deleteInvitation(invitationId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deleting invitation with ID {}: {}", invitationId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/cancel/{eventId}/{userEmail}")
    public ResponseEntity<Void> cancelUserRegistration(
            @PathVariable Long eventId,
            @PathVariable String userEmail) {
        log.info("Cancelling registration for user {} in event {}", userEmail, eventId);
        try {
            invitationService.cancelUserRegistration(eventId, userEmail);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error cancelling registration for user {} in event {}: {}", userEmail, eventId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/event/{eventId}/confirmed-count")
    public ResponseEntity<Long> getConfirmedInvitationsCount(@PathVariable Long eventId) {
        log.info("Getting confirmed invitations count for event {}", eventId);
        try {
            Long count = invitationRepository.countConfirmedInvitations(eventId);
            log.info("Confirmed invitations count for event {}: {}", eventId, count);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting confirmed invitations count for event {}: {}", eventId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/test/create-waitlist/{eventId}/{userEmail}")
    public ResponseEntity<String> createTestWaitlistInvitation(
            @PathVariable Long eventId,
            @PathVariable String userEmail) {
        log.info("TEST: Creating WAITLIST invitation for user {} in event {}", userEmail, eventId);
        try {
            // Simuler le message Kafka
            String testMessage = String.format(
                "{\"eventId\":%d,\"eventTitle\":\"Test Event\",\"userEmail\":\"%s\"}",
                eventId, userEmail
            );

            // Appeler directement le handler
            invitationService.handleWaitlistInvitationCreation(testMessage);

            return ResponseEntity.ok("Test WAITLIST invitation created successfully");
        } catch (Exception e) {
            log.error("Error creating test WAITLIST invitation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/admin/clear-all")
    public ResponseEntity<String> clearAllInvitations() {
        log.info("ADMIN: Clearing all invitations");
        try {
            long deletedCount = invitationService.clearAllInvitations();

            // Envoyer message Kafka pour vider aussi la liste d'attente
            String clearWaitlistMessage = "{\"action\":\"CLEAR_ALL_WAITLISTS\"}";
            kafkaTemplate.send("waitlist.clear.all", clearWaitlistMessage);

            return ResponseEntity.ok(String.format("Successfully cleared %d invitations and triggered waitlist cleanup", deletedCount));
        } catch (Exception e) {
            log.error("Error clearing all invitations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    private InvitationResponse toResponse(InvitationEntity entity) {
        return InvitationResponse.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .eventTitle(entity.getEventTitle())
                .userEmail(entity.getUserEmail())
                .status(entity.getStatus().name())
                .seatInfo(entity.getSeatInfo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
} 