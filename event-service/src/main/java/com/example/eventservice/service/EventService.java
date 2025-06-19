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
     * Créer un nouvel événement (accessible uniquement par l'admin)
     * @param event L'événement à créer
     * @param adminId L'ID de l'administrateur qui crée l'événement
     * @return L'événement créé
     * @throws IllegalArgumentException Si l'admin n'est pas valide
     */
    public EventEntity createEvent(EventEntity event, Long adminId) {
        logger.info("Création d'un nouvel événement par l'admin {} : {}", adminId, event.getTitle());

        // S'assurer que l'ID de l'organisateur est bien celui de l'admin
        if (!adminId.equals(event.getOrganizerId())) {
            logger.error("Tentative de création d'événement avec un organizerId différent de l'admin connecté");
            throw new IllegalArgumentException("L'organisateur doit être l'administrateur connecté");
        }

        EventEntity savedEvent = eventRepository.save(event);

        // Publier l'événement Kafka
        publishEventCreated(savedEvent);

        logger.info("Événement créé avec succès avec l'ID : {}", savedEvent.getId());
        return savedEvent;
    }

    /**
     * Mettre à jour un événement existant (uniquement par l'admin)
     * @param id L'ID de l'événement à mettre à jour
     * @param eventDetails Les nouvelles données de l'événement
     * @param adminId L'ID de l'administrateur qui effectue la mise à jour
     * @return L'événement mis à jour
     * @throws IllegalArgumentException Si l'admin n'est pas l'organisateur
     */
    public EventEntity updateEvent(Long id, EventEntity eventDetails, Long adminId) {
        logger.info("Mise à jour de l'événement avec l'ID : {} par l'admin : {}", id, adminId);

        EventEntity event = getEventById(id);
        
        // Vérifier que l'admin est bien l'organisateur
        if (!adminId.equals(event.getOrganizerId())) {
            logger.error("Tentative de mise à jour d'un événement par un non-organisateur. Admin: {}, Organisateur: {}", 
                adminId, event.getOrganizerId());
            throw new IllegalArgumentException("Seul l'organisateur peut modifier l'événement");
        }
        
        // Mettre à jour uniquement les champs existants dans EventEntity
        event.setTitle(eventDetails.getTitle());
        event.setDescription(eventDetails.getDescription());
        event.setLocation(eventDetails.getLocation());
        event.setDateTime(eventDetails.getDateTime());
        // Mettre à jour la date de modification
        event.setUpdatedAt(LocalDateTime.now());

        EventEntity updatedEvent = eventRepository.save(event);
        
        // Publier l'événement Kafka
        publishEventUpdated(updatedEvent);
        
        logger.info("Événement mis à jour avec succès avec l'ID : {}", updatedEvent.getId());
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
     * Obtenir un événement par son ID
     * @param id L'ID de l'événement à récupérer
     * @return L'entité événement
     * @throws RuntimeException si l'événement n'est pas trouvé
     */
    public EventEntity getEventById(Long id) {
        logger.info("Récupération de l'événement avec l'ID : {}", id);
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
