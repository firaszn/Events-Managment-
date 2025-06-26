package com.example.invitationservice.controller;

import com.example.invitationservice.entity.InvitationEntity;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
@Slf4j
public class InvitationController {

    private final InvitationService invitationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${kafka.topics.invitation-responded}")
    private String invitationRespondedTopic;

    @GetMapping("/check/{eventId}/{userEmail}")
    public ResponseEntity<Boolean> isUserRegisteredForEvent(
            @PathVariable Long eventId,
            @PathVariable String userEmail) {
        log.info("Received registration check request for event {} and user {}", eventId, userEmail);
        
        try {
            boolean isRegistered = invitationService.isUserRegisteredForEvent(eventId, userEmail);
            log.info("Registration check result for event {} and user {}: {}", eventId, userEmail, isRegistered);
            return ResponseEntity.ok(isRegistered);
        } catch (Exception e) {
            log.error("Error checking registration for event {} and user {}: {}", eventId, userEmail, e.getMessage());
            log.error("Full error details:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    @PostMapping
    public ResponseEntity<InvitationResponse> registerForEvent(
            @Valid @RequestBody InvitationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            log.debug("Received registration request for event: {}", request.getEventId());
            
            // Utiliser l'email de l'utilisateur authentifié
            String userEmail = jwt.getClaim("email");
            log.debug("User email from JWT: {}", userEmail);
            request.setUserEmail(userEmail);
            
            InvitationEntity invitation = invitationService.createInvitation(request);
            return ResponseEntity.ok(toResponse(invitation));
        } catch (RuntimeException e) {
            log.error("Error creating invitation", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    @PostMapping("/test-kafka")
    public ResponseEntity<String> testKafka(@RequestBody String message) {
        try {
            Map<String, Object> testMessage = new HashMap<>();
            testMessage.put("message", message);
            testMessage.put("timestamp", System.currentTimeMillis());
            
            kafkaTemplate.send(invitationRespondedTopic, testMessage);
            return ResponseEntity.ok("Message envoyé avec succès : " + message);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lors de l'envoi du message : " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<InvitationResponse>> getAllInvitations() {
        List<InvitationEntity> invitations = invitationService.getAllInvitations();
        List<InvitationResponse> responses = invitations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private InvitationResponse toResponse(InvitationEntity entity) {
        return InvitationResponse.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .eventTitle(entity.getEventTitle())
                .userEmail(entity.getUserEmail())
                .status(entity.getStatus().name())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
} 