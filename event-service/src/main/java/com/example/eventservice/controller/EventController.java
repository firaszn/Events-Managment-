package com.example.eventservice.controller;

import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.mapper.EventMapper;
import com.example.eventservice.model.*;
import com.example.eventservice.service.EventService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST pour la gestion des événements
 *
 * Endpoints :
 * - POST /events – Créer événement
 * - GET /events/{id} – Détails
 * - PUT /events/{id} – Modifier
 * - DELETE /events/{id} – Supprimer
 * - GET /events?organizerId=x – Événements par utilisateur
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    @Autowired
    private EventService eventService;

    @Autowired
    private EventMapper eventMapper;

    /**
     * POST /events – Créer un événement
     */
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @RequestHeader("X-User-Id") Long adminId,
            @RequestHeader("X-User-Roles") String roles,
            @Valid @RequestBody EventRequest eventRequest) {

        logger.info("Requête de création d'événement reçue de l'utilisateur : {}", adminId);

        // Vérifier si l'utilisateur est un admin
        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            logger.warn("Tentative de création d'événement par un utilisateur non admin: {}", adminId);
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        try {
            // Créer l'événement avec l'admin comme organisateur
            EventEntity eventEntity = eventMapper.toEventEntity(eventRequest);
            eventEntity.setOrganizerId(adminId);

            // Appeler le service avec l'ID de l'admin pour validation
            EventEntity createdEvent = eventService.createEvent(eventEntity, adminId);
            EventResponse response = eventMapper.toEventResponse(createdEvent);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Erreur lors de la création de l'événement", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /events/{id} – Obtenir les détails d'un événement
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        logger.info("Requête de récupération d'événement reçue pour l'ID : {}", id);

        try {
            EventEntity event = eventService.getEventById(id);
            EventResponse response = eventMapper.toEventResponse(event);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            logger.error("Événement non trouvé avec l'ID : {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'événement", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /events/{id} – Modifier un événement
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long adminId,
            @RequestHeader("X-User-Roles") String roles,
            @Valid @RequestBody EventUpdateRequest updateRequest) {

        logger.info("Requête de mise à jour d'événement reçue pour l'ID : {} par l'admin : {}", id, adminId);

        // Vérifier si l'utilisateur est un admin
        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            logger.warn("Tentative de mise à jour d'événement par un utilisateur non admin: {}", adminId);
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        try {
            // Récupérer l'événement existant
            EventEntity existingEvent = eventService.getEventById(id);

            // Vérifier que l'admin est bien l'organisateur
            if (!adminId.equals(existingEvent.getOrganizerId())) {
                logger.warn("Tentative de modification d'un événement par un non-organisateur. Admin: {}, Organisateur: {}",
                    adminId, existingEvent.getOrganizerId());
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            // Créer une nouvelle entité avec les valeurs mises à jour
            EventEntity updatedEvent = new EventEntity();
            updatedEvent.setTitle(updateRequest.getTitle());
            updatedEvent.setDescription(updateRequest.getDescription());
            updatedEvent.setLocation(updateRequest.getLocation());
            updatedEvent.setDateTime(updateRequest.getDateTime());

            // Appeler le service avec l'ID de l'admin pour validation
            EventEntity savedEvent = eventService.updateEvent(id, updatedEvent, adminId);
            EventResponse response = eventMapper.toEventResponse(savedEvent);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            logger.error("Événement non trouvé avec l'ID : {}", id, e);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour de l'événement", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DELETE /events/{id} – Supprimer un événement (admin uniquement)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long adminId,
            @RequestHeader("X-User-Roles") String roles) {

        logger.info("Requête de suppression d'événement reçue pour l'ID : {} par l'admin : {}", id, adminId);

        // Vérifier si l'utilisateur est un admin
        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            logger.warn("Tentative de suppression d'événement par un utilisateur non admin: {}", adminId);
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        try {
            // Vérifier que l'admin est bien l'organisateur
            EventEntity event = eventService.getEventById(id);
            if (!adminId.equals(event.getOrganizerId())) {
                logger.warn("Tentative de suppression d'un événement par un non-organisateur. Admin: {}, Organisateur: {}",
                    adminId, event.getOrganizerId());
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            eventService.deleteEvent(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            logger.error("Événement non trouvé avec l'ID : {}", id, e);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de l'événement", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /events – Obtenir tous les événements ou filtrer par organisateur
     */
    @GetMapping
    public ResponseEntity<List<EventResponse>> getEvents(@RequestParam(required = false) Long organizerId,
                                                 @RequestParam(required = false) String title,
                                                 @RequestParam(required = false) String location,
                                                 @RequestParam(required = false, defaultValue = "false") boolean upcoming) {
        logger.info("Requête de récupération d'événements reçue - organizerId: {}, title: {}, location: {}, upcoming: {}",
                   organizerId, title, location, upcoming);

        try {
            List<EventEntity> events;

            if (organizerId != null) {
                if (upcoming) {
                    events = eventService.getUpcomingEventsByOrganizer(organizerId);
                } else {
                    events = eventService.getEventsByOrganizer(organizerId);
                }
            } else if (title != null) {
                events = eventService.searchEventsByTitle(title);
            } else if (location != null) {
                events = eventService.searchEventsByLocation(location);
            } else if (upcoming) {
                events = eventService.getUpcomingEvents();
            } else {
                events = eventService.getAllEvents();
            }

            List<EventResponse> response = eventMapper.toEventResponseList(events);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des événements", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /events/upcoming – Obtenir tous les événements futurs
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents() {
        logger.info("Requête de récupération des événements futurs reçue");

        try {
            List<EventEntity> events = eventService.getUpcomingEvents();
            List<EventResponse> response = eventMapper.toEventResponseList(events);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des événements futurs", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /events/organizer/{organizerId} – Obtenir les événements d'un organisateur
     */
    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<List<EventResponse>> getEventsByOrganizer(@PathVariable Long organizerId) {
        logger.info("Requête de récupération des événements pour l'organisateur : {}", organizerId);

        try {
            List<EventEntity> events = eventService.getEventsByOrganizer(organizerId);
            List<EventResponse> response = eventMapper.toEventResponseList(events);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des événements de l'organisateur", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
