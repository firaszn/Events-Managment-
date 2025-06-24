package com.example.eventservice.controller;

import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.mapper.EventMapper;
import com.example.eventservice.model.EventRequest;
import com.example.eventservice.model.EventResponse;
import com.example.eventservice.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventMapper eventMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest eventRequest, @AuthenticationPrincipal Jwt jwt) {
        EventEntity eventEntity = eventMapper.toEntity(eventRequest);
        eventEntity.setOrganizerId(jwt.getSubject());
        EventEntity createdEvent = eventService.createEvent(eventEntity);
        return new ResponseEntity<>(eventMapper.toResponse(createdEvent), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        List<EventEntity> events = eventService.getAllEvents();
        return ResponseEntity.ok(eventMapper.toResponseList(events));
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