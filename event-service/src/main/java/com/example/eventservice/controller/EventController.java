package com.example.eventservice.controller;

import com.example.eventservice.client.InvitationClient;
import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.mapper.EventMapper;
import com.example.eventservice.model.EventRequest;
import com.example.eventservice.model.EventResponse;
import com.example.eventservice.service.EventService;
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
                return response;
            })
            .toList();
        
        logger.info("Returning {} events", responses.size());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return eventService.getEventById(id)
                .map(eventMapper::toResponse)
                .map(ResponseEntity::ok)
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
} 