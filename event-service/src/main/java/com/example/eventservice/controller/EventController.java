package com.example.eventservice.controller;

import com.example.eventservice.client.InvitationClient;
import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.mapper.EventMapper;
import com.example.eventservice.model.EventRequest;
import com.example.eventservice.model.EventResponse;
import com.example.eventservice.service.EventService;
import com.example.eventservice.service.EventReminderService;
import com.example.eventservice.service.WaitlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventMapper eventMapper;
    private final InvitationClient invitationClient;
    private final EventReminderService eventReminderService;
    private final WaitlistService waitlistService;
    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest eventRequest, @AuthenticationPrincipal Jwt jwt) {
        EventEntity eventEntity = eventMapper.toEntity(eventRequest);
        eventEntity.setOrganizerId(jwt.getSubject());
        EventEntity createdEvent = eventService.createEvent(eventEntity);
        return new ResponseEntity<>(eventMapper.toResponse(createdEvent), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents(@AuthenticationPrincipal Jwt jwt) {
        List<EventEntity> events = eventService.getAllEvents();
        String userEmail = jwt.getClaim("email");
        
        logger.info("Getting all events for user: {}", userEmail);
        logger.debug("JWT token subject: {}", jwt.getSubject());
        logger.debug("JWT token claims: {}", jwt.getClaims());
        
        List<EventResponse> responses = events.stream()
            .map(event -> {
                EventResponse response = eventMapper.toResponse(event);

                // Log des valeurs importantes pour debug
                logger.info("Event {}: maxCapacity={}, waitlistEnabled={}",
                           event.getTitle(), event.getMaxCapacity(), event.getWaitlistEnabled());
                
                try {
                    logger.info("Checking registration for event ID: {} ({})", event.getId(), event.getTitle());
                    
                    ResponseEntity<Boolean> registrationResponse = invitationClient.isUserRegisteredForEvent(event.getId(), userEmail);
                    
                    if (registrationResponse.getBody() != null) {
                        boolean isRegistered = registrationResponse.getBody();
                        logger.info("User {} is {} registered for event {}", 
                                  userEmail, isRegistered ? "" : "not", event.getId());
                        response.setUserRegistered(isRegistered);
                    } else {
                        logger.warn("Received null response body for event {} registration check", event.getId());
                        response.setUserRegistered(false);
                    }
                } catch (Exception e) {
                    logger.error("Error checking registration for event {}: {}", event.getId(), e.getMessage());
                    logger.error("Full error details:", e);
                    response.setUserRegistered(false);
                }

                // Vérifier si l'utilisateur a une invitation en attente
                try {
                    ResponseEntity<Boolean> pendingResponse = invitationClient.hasUserPendingInvitation(event.getId(), userEmail);
                    if (pendingResponse.getBody() != null) {
                        response.setUserHasPendingInvitation(pendingResponse.getBody());
                    } else {
                        response.setUserHasPendingInvitation(false);
                    }
                } catch (Exception e) {
                    logger.error("Error checking pending invitation for event {}: {}", event.getId(), e.getMessage());
                    response.setUserHasPendingInvitation(false);
                }

                // Ajout : Récupérer l'invitation de l'utilisateur pour ce event
                try {
                    List<com.example.eventservice.model.InvitationResponse> allInvitations = invitationClient.getAllInvitations().getBody();
                    if (allInvitations != null) {
                        allInvitations.stream()
                            .filter(inv -> inv.getEventId().equals(event.getId()) && userEmail.equals(inv.getUserEmail()))
                            .findFirst()
                            .ifPresent(inv -> {
                                if ("CANCELLED".equals(inv.getStatus())) {
                                    response.setUserStatus("CANCELLED");
                                }
                            });
                    }
                } catch (Exception e) {
                    logger.error("Error fetching invitation status for event {}: {}", event.getId(), e.getMessage());
                }

                // Ajouter les informations de liste d'attente
                try {
                    response.setConfirmedParticipants(waitlistService.getConfirmedParticipantsCount(event.getId()));
                    response.setWaitlistCount(waitlistService.getWaitlistCount(event.getId()));

                    // Position et statut de l'utilisateur dans la liste d'attente (si applicable)
                    waitlistService.getUserWaitlistPosition(event.getId(), userEmail)
                            .ifPresent(waitlistResponse -> {
                                response.setUserWaitlistPosition(waitlistResponse.getPosition());
                                response.setUserWaitlistStatus(waitlistResponse.getStatus());
                            });
                } catch (Exception e) {
                    logger.error("Error fetching waitlist information for event {}: {}", event.getId(), e.getMessage());
                    response.setConfirmedParticipants(0L);
                    response.setWaitlistCount(0L);
                }
                
                return response;
            })
            .toList();
        
        logger.info("Returning {} events", responses.size());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return eventService.getEventById(id)
                .map(event -> {
                    EventResponse response = eventMapper.toResponse(event);
                    String userEmail = jwt != null ? jwt.getClaim("email") : null;
                    
                    if (userEmail != null) {
                        // Vérifier l'inscription de l'utilisateur
                        try {
                            ResponseEntity<Boolean> registrationResponse = invitationClient.isUserRegisteredForEvent(event.getId(), userEmail);
                            response.setUserRegistered(Boolean.TRUE.equals(registrationResponse.getBody()));
                        } catch (Exception e) {
                            logger.error("Error checking registration for event {}: {}", event.getId(), e.getMessage());
                            response.setUserRegistered(false);
                        }

                        // Vérifier si l'utilisateur a une invitation en attente
                        try {
                            ResponseEntity<Boolean> pendingResponse = invitationClient.hasUserPendingInvitation(event.getId(), userEmail);
                            if (pendingResponse.getBody() != null) {
                                response.setUserHasPendingInvitation(pendingResponse.getBody());
                            } else {
                                response.setUserHasPendingInvitation(false);
                            }
                        } catch (Exception e) {
                            logger.error("Error checking pending invitation for event {}: {}", event.getId(), e.getMessage());
                            response.setUserHasPendingInvitation(false);
                        }
                        
                        // Ajout : Récupérer l'invitation de l'utilisateur pour ce event
                        try {
                            List<com.example.eventservice.model.InvitationResponse> allInvitations = invitationClient.getAllInvitations().getBody();
                            if (allInvitations != null) {
                                allInvitations.stream()
                                    .filter(inv -> inv.getEventId().equals(event.getId()) && userEmail.equals(inv.getUserEmail()))
                                    .findFirst()
                                    .ifPresent(inv -> {
                                        if ("CANCELLED".equals(inv.getStatus())) {
                                            response.setUserStatus("CANCELLED");
                                        }
                                    });
                            }
                        } catch (Exception e) {
                            logger.error("Error fetching invitation status for event {}: {}", event.getId(), e.getMessage());
                        }
                        
                        // Ajouter les informations de liste d'attente
                        try {
                            response.setConfirmedParticipants(waitlistService.getConfirmedParticipantsCount(event.getId()));
                            response.setWaitlistCount(waitlistService.getWaitlistCount(event.getId()));

                            waitlistService.getUserWaitlistPosition(event.getId(), userEmail)
                                    .ifPresent(waitlistResponse -> {
                                        response.setUserWaitlistPosition(waitlistResponse.getPosition());
                                        response.setUserWaitlistStatus(waitlistResponse.getStatus());
                                    });
                        } catch (Exception e) {
                            logger.error("Error fetching waitlist information for event {}: {}", event.getId(), e.getMessage());
                            response.setConfirmedParticipants(0L);
                            response.setWaitlistCount(0L);
                        }
                    }
                    
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable Long id, @Valid @RequestBody EventRequest eventRequest) {
        EventEntity updatedEvent = eventService.updateEvent(id, eventRequest);
        return ResponseEntity.ok(eventMapper.toResponse(updatedEvent));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        if (eventService.getEventById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint de test pour déclencher manuellement les rappels d'événements
     * Utile pour les tests et la démonstration
     */
    @PostMapping("/test-reminders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerEventReminders() {
        try {
            logger.info("Déclenchement manuel des rappels d'événements");
            eventReminderService.sendEventReminders();
            return ResponseEntity.ok("Rappels d'événements déclenchés avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors du déclenchement manuel des rappels : {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du déclenchement des rappels : " + e.getMessage());
        }
    }
} 