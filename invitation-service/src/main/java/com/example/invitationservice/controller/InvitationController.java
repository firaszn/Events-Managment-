package com.example.invitationservice.controller;

import com.example.invitationservice.entity.InvitationEntity;
import com.example.invitationservice.entity.SeatInfo;
import com.example.invitationservice.model.InvitationRequest;
import com.example.invitationservice.model.InvitationResponse;
import com.example.invitationservice.service.InvitationService;
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

@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
@Slf4j
public class InvitationController {

    private final InvitationService invitationService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
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
        return ResponseEntity.ok(occupiedSeats);
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