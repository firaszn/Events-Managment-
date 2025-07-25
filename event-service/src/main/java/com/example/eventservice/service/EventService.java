package com.example.eventservice.service;

import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.model.EventRequest;
import com.example.eventservice.repository.EventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final WaitlistService waitlistService;

    public EventEntity createEvent(EventEntity event) {
        // Forcer les valeurs par défaut
        event.setMaxCapacity(5); // 5 places par défaut
        event.setWaitlistEnabled(true); // Liste d'attente activée par défaut

        return eventRepository.save(event);
    }

    public List<EventEntity> getAllEvents() {
        List<EventEntity> events = eventRepository.findAll();

        // Mettre à jour les événements qui n'ont pas les valeurs par défaut
        for (EventEntity event : events) {
            boolean needsUpdate = false;

            if (event.getMaxCapacity() == null) {
                event.setMaxCapacity(5);
                needsUpdate = true;
            }

            if (event.getWaitlistEnabled() == null) {
                event.setWaitlistEnabled(true);
                needsUpdate = true;
            }

            if (needsUpdate) {
                eventRepository.save(event);
                log.info("Mise à jour des valeurs par défaut pour l'événement: {}", event.getTitle());
            }
        }

        return events;
    }

    public Optional<EventEntity> getEventById(Long id) {
        return eventRepository.findById(id);
    }

    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    public EventEntity updateEvent(Long id, EventRequest eventRequest) {
        EventEntity existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));

        existingEvent.setTitle(eventRequest.getTitle());
        existingEvent.setDescription(eventRequest.getDescription());
        existingEvent.setEventDate(eventRequest.getEventDate());
        existingEvent.setLocation(eventRequest.getLocation());
        
        // Forcer les valeurs par défaut même en modification
        existingEvent.setMaxCapacity(5); // 5 places par défaut
        existingEvent.setWaitlistEnabled(true); // Liste d'attente activée par défaut

        return eventRepository.save(existingEvent);
    }

    /**
     * Vérifier si un événement est complet
     */
    public boolean isEventFull(Long eventId) {
        return waitlistService.isEventFull(eventId);
    }

    /**
     * Obtenir le nombre de participants confirmés
     */
    public long getConfirmedParticipantsCount(Long eventId) {
        return waitlistService.getConfirmedParticipantsCount(eventId);
    }

    /**
     * Obtenir le nombre de personnes en liste d'attente
     */
    public long getWaitlistCount(Long eventId) {
        return waitlistService.getWaitlistCount(eventId);
    }
} 