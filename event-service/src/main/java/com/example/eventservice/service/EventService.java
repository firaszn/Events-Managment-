package com.example.eventservice.service;

import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service métier pour la gestion des événements
 */
@Service
@Transactional
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // Topics Kafka
    private static final String EVENT_CREATED_TOPIC = "event.created";
    private static final String EVENT_UPDATED_TOPIC = "event.updated";

    /**
     * Créer un nouvel événement
     */
    public EventEntity createEvent(EventEntity event) {
        logger.info("Création d'un nouvel événement : {}", event.getTitle());

        EventEntity savedEvent = eventRepository.save(event);

        // Publier l'événement Kafka
        publishEventCreated(savedEvent);

        logger.info("Événement créé avec succès avec l'ID : {}", savedEvent.getId());
        return savedEvent;
    }

    /**
     * Mettre à jour un événement existant
     */
    public EventEntity updateEvent(Long id, EventEntity eventDetails) {
        logger.info("Mise à jour de l'événement avec l'ID : {}", id);

        Optional<EventEntity> optionalEvent = eventRepository.findById(id);
        if (optionalEvent.isEmpty()) {
            throw new RuntimeException("Événement non trouvé avec l'ID : " + id);
        }

        EventEntity existingEvent = optionalEvent.get();

        // Mettre à jour les champs
        existingEvent.setTitle(eventDetails.getTitle());
        existingEvent.setDescription(eventDetails.getDescription());
        existingEvent.setLocation(eventDetails.getLocation());
        existingEvent.setDateTime(eventDetails.getDateTime());

        EventEntity updatedEvent = eventRepository.save(existingEvent);

        // Publier l'événement Kafka
        publishEventUpdated(updatedEvent);

        logger.info("Événement mis à jour avec succès : {}", updatedEvent.getId());
        return updatedEvent;
    }

    /**
     * Supprimer un événement
     */
    public void deleteEvent(Long id) {
        logger.info("Suppression de l'événement avec l'ID : {}", id);
        
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Événement non trouvé avec l'ID : " + id);
        }
        
        eventRepository.deleteById(id);
        logger.info("Événement supprimé avec succès : {}", id);
    }

    /**
     * Obtenir un événement par ID
     */
    @Transactional(readOnly = true)
    public EventEntity getEventById(Long id) {
        logger.info("Recherche de l'événement avec l'ID : {}", id);

        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé avec l'ID : " + id));
    }

    /**
     * Obtenir tous les événements
     */
    @Transactional(readOnly = true)
    public List<EventEntity> getAllEvents() {
        logger.info("Récupération de tous les événements");
        return eventRepository.findAll();
    }

    /**
     * Obtenir les événements d'un organisateur
     */
    @Transactional(readOnly = true)
    public List<EventEntity> getEventsByOrganizer(Long organizerId) {
        logger.info("Récupération des événements pour l'organisateur : {}", organizerId);
        return eventRepository.findByOrganizerIdOrderByDateTimeAsc(organizerId);
    }

    /**
     * Obtenir les événements futurs d'un organisateur
     */
    @Transactional(readOnly = true)
    public List<EventEntity> getUpcomingEventsByOrganizer(Long organizerId) {
        logger.info("Récupération des événements futurs pour l'organisateur : {}", organizerId);
        return eventRepository.findUpcomingEventsByOrganizer(organizerId, LocalDateTime.now());
    }

    /**
     * Obtenir tous les événements futurs
     */
    @Transactional(readOnly = true)
    public List<EventEntity> getUpcomingEvents() {
        logger.info("Récupération de tous les événements futurs");
        return eventRepository.findUpcomingEvents(LocalDateTime.now());
    }

    /**
     * Rechercher des événements par titre
     */
    @Transactional(readOnly = true)
    public List<EventEntity> searchEventsByTitle(String title) {
        logger.info("Recherche d'événements par titre : {}", title);
        return eventRepository.findByTitleContainingIgnoreCase(title);
    }

    /**
     * Rechercher des événements par lieu
     */
    @Transactional(readOnly = true)
    public List<EventEntity> searchEventsByLocation(String location) {
        logger.info("Recherche d'événements par lieu : {}", location);
        return eventRepository.findByLocationContainingIgnoreCase(location);
    }

    /**
     * Publier un événement de création sur Kafka
     */
    private void publishEventCreated(EventEntity event) {
        try {
            kafkaTemplate.send(EVENT_CREATED_TOPIC, event);
            logger.info("Événement de création publié sur Kafka pour l'événement : {}", event.getId());
        } catch (Exception e) {
            logger.error("Erreur lors de la publication de l'événement de création sur Kafka", e);
        }
    }

    /**
     * Publier un événement de mise à jour sur Kafka
     */
    private void publishEventUpdated(EventEntity event) {
        try {
            kafkaTemplate.send(EVENT_UPDATED_TOPIC, event);
            logger.info("Événement de mise à jour publié sur Kafka pour l'événement : {}", event.getId());
        } catch (Exception e) {
            logger.error("Erreur lors de la publication de l'événement de mise à jour sur Kafka", e);
        }
    }
}
