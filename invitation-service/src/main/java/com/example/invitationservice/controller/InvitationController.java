package com.example.invitationservice.controller;

import com.example.invitationservice.entity.InvitationEntity;
import com.example.invitationservice.entity.SeatInfo;
import com.example.invitationservice.entity.TemporarySeatLock;
import com.example.invitationservice.model.InvitationRequest;
import com.example.invitationservice.model.InvitationResponse;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
@Slf4j
public class

InvitationController {

    private final InvitationService invitationService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SeatLockService seatLockService;
    
    @Value("${kafka.topics.invitation-responded}")
    private String invitationRespondedTopic;

    @GetMapping
    public ResponseEntity<List<InvitationResponse>> getAllInvitations() {
        log.info("Fetching all invitations");
        List<InvitationEntity> invitations = invitationService.getAllInvitations();
        List<InvitationResponse> responses = invitations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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

    @GetMapping("/event/{eventId}/occupied-seats")
    public ResponseEntity<List<SeatInfo>> getOccupiedSeats(@PathVariable String eventId) {
        Long eventIdLong = Long.parseLong(eventId);
        List<SeatInfo> occupiedSeats = invitationService.getOccupiedSeatsForEvent(eventIdLong);
        
        // Ajouter les places temporairement verrouillées
        List<TemporarySeatLock> lockedSeats = seatLockService.getLockedSeats(eventIdLong);
        
        for (TemporarySeatLock lock : lockedSeats) {
            if (!lock.isExpired()) {
                boolean alreadyOccupied = occupiedSeats.stream()
                    .anyMatch(seat -> seat.getRow().equals(lock.getRow()) && 
                                    seat.getNumber().equals(lock.getNumber()));
                
                if (!alreadyOccupied) {
                    occupiedSeats.add(SeatInfo.builder()
                        .row(lock.getRow())
                        .number(lock.getNumber())
                        .build());
                }
            }
        }
        
        return ResponseEntity.ok(occupiedSeats);
    }

    @PostMapping("/event/{eventId}/lock-seat")
    public ResponseEntity<Void> lockSeat(
            @PathVariable String eventId,
            @RequestBody SeatInfo seatInfo,
            @AuthenticationPrincipal Jwt jwt) {
        
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
    }

    @DeleteMapping("/event/{eventId}/release-seat")
    public ResponseEntity<Void> releaseSeat(
            @PathVariable String eventId,
            @RequestBody SeatInfo seatInfo,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userEmail = jwt.getClaimAsString("email");
        seatLockService.releaseSeat(
            Long.parseLong(eventId),
            seatInfo.getRow(),
            seatInfo.getNumber(),
            userEmail
        );

        return ResponseEntity.ok().build();
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