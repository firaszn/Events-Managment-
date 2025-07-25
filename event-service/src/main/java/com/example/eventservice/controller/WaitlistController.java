package com.example.eventservice.controller;

import com.example.eventservice.model.WaitlistResponse;
import com.example.eventservice.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/events/{eventId}/waitlist")
@RequiredArgsConstructor
@Slf4j
public class WaitlistController {

    private final WaitlistService waitlistService;

    /**
     * Rejoindre la liste d'attente d'un événement
     */
    @PostMapping("/join")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> joinWaitlist(@PathVariable Long eventId, @AuthenticationPrincipal Jwt jwt) {
        try {
            String userEmail = jwt.getClaim("email");
            log.info("Utilisateur {} demande à rejoindre la liste d'attente de l'événement {}", userEmail, eventId);
            
            WaitlistResponse response = waitlistService.joinWaitlist(eventId, userEmail);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Erreur de validation pour la liste d'attente : {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("État invalide pour rejoindre la liste d'attente : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout à la liste d'attente", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur interne du serveur");
        }
    }

    /**
     * Quitter la liste d'attente d'un événement
     */
    @DeleteMapping("/leave")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> leaveWaitlist(@PathVariable Long eventId, @AuthenticationPrincipal Jwt jwt) {
        try {
            String userEmail = jwt.getClaim("email");
            log.info("Utilisateur {} demande à quitter la liste d'attente de l'événement {}", userEmail, eventId);
            
            waitlistService.leaveWaitlist(eventId, userEmail);
            return ResponseEntity.ok().body("Vous avez quitté la liste d'attente");
            
        } catch (IllegalArgumentException e) {
            log.warn("Erreur lors de la sortie de la liste d'attente : {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Erreur lors de la sortie de la liste d'attente", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur interne du serveur");
        }
    }

    /**
     * Obtenir la position dans la liste d'attente
     */
    @GetMapping("/position")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getWaitlistPosition(@PathVariable Long eventId, @AuthenticationPrincipal Jwt jwt) {
        try {
            String userEmail = jwt.getClaim("email");
            Optional<WaitlistResponse> position = waitlistService.getUserWaitlistPosition(eventId, userEmail);
            
            if (position.isPresent()) {
                return ResponseEntity.ok(position.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la position en liste d'attente", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur interne du serveur");
        }
    }

    /**
     * Confirmer une place depuis la liste d'attente
     */
    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> confirmWaitlistSpot(@PathVariable Long eventId, @AuthenticationPrincipal Jwt jwt) {
        try {
            String userEmail = jwt.getClaim("email");
            log.info("Utilisateur {} confirme sa place depuis la liste d'attente pour l'événement {}", userEmail, eventId);
            
            waitlistService.confirmWaitlistSpot(eventId, userEmail);
            return ResponseEntity.ok().body("Place confirmée avec succès");
            
        } catch (IllegalArgumentException e) {
            log.warn("Erreur lors de la confirmation : {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("État invalide pour la confirmation : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erreur lors de la confirmation de la place", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur interne du serveur");
        }
    }

    /**
     * Obtenir le nombre de personnes en liste d'attente (admin uniquement)
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> getWaitlistCount(@PathVariable Long eventId) {
        try {
            long count = waitlistService.getWaitlistCount(eventId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du nombre en liste d'attente", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Déclencher manuellement la redistribution (admin uniquement)
     */
    @PostMapping("/redistribute/{slots}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> redistributeSlots(@PathVariable Long eventId, @PathVariable int slots) {
        try {
            log.info("Redistribution manuelle de {} place(s) pour l'événement {}", slots, eventId);
            waitlistService.redistributeAvailableSlots(eventId, slots);
            return ResponseEntity.ok().body("Redistribution effectuée avec succès");
        } catch (Exception e) {
            log.error("Erreur lors de la redistribution manuelle", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur interne du serveur");
        }
    }

    @PostMapping("/test/kafka/{userEmail}")
    public ResponseEntity<String> testKafkaMessage(
            @PathVariable Long eventId,
            @PathVariable String userEmail) {
        try {
            log.info("TEST: Envoi message Kafka pour création invitation WAITLIST");

            // Simuler l'ajout à la liste d'attente et création d'invitation
            WaitlistResponse response = waitlistService.joinWaitlist(eventId, userEmail);

            return ResponseEntity.ok("Test Kafka réussi - Position: " + response.getPosition());
        } catch (Exception e) {
            log.error("Erreur test Kafka: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur test Kafka: " + e.getMessage());
        }
    }
} 