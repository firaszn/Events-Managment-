package com.example.eventservice.service;

import com.example.eventservice.client.InvitationClient;
import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.model.EventReminderMessage;
import com.example.eventservice.model.InvitationResponse;
import com.example.eventservice.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventReminderService {

    private final EventRepository eventRepository;
    private final InvitationClient invitationClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${kafka.topics.event-reminder:event.reminder}")
    private String eventReminderTopic;

    /**
     * Vérifie les événements qui commencent dans 1 heure et envoie des rappels
     * S'exécute toutes les 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes = 600,000 ms
    public void sendEventReminders() {
        try {
            log.info("=== DÉBUT du traitement des rappels d'événements ===");
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourFromNow = now.plusHours(1);
            
            // Fenêtre de 10 minutes pour capturer les événements 
            // qui commencent entre 50 et 70 minutes à partir de maintenant
            LocalDateTime startWindow = now.plusMinutes(50);
            LocalDateTime endWindow = now.plusMinutes(70);
            
            log.info("Recherche d'événements entre {} et {}", startWindow, endWindow);
            
            // Récupérer les événements qui commencent dans cette fenêtre
            List<EventEntity> upcomingEvents = eventRepository.findEventsInTimeRange(startWindow, endWindow);
            
            log.info("Trouvé {} événement(s) nécessitant un rappel", upcomingEvents.size());
            
            for (EventEntity event : upcomingEvents) {
                try {
                    sendReminderForEvent(event);
                } catch (Exception e) {
                    log.error("Erreur lors de l'envoi du rappel pour l'événement {}: {}", 
                             event.getId(), e.getMessage(), e);
                }
            }
            
            log.info("=== FIN du traitement des rappels d'événements ===");
            
        } catch (Exception e) {
            log.error("Erreur globale lors du traitement des rappels d'événements: {}", e.getMessage(), e);
        }
    }

    private void sendReminderForEvent(EventEntity event) {
        try {
            log.info("Traitement du rappel pour l'événement: {} - {}", event.getId(), event.getTitle());
            
            // Récupérer toutes les invitations
            List<InvitationResponse> allInvitations = invitationClient.getAllInvitations()
                .getBody();
            
            if (allInvitations == null) {
                log.warn("Impossible de récupérer les invitations pour l'événement {}", event.getId());
                return;
            }
            
            // Filtrer les participants confirmés pour cet événement
            List<String> participantEmails = allInvitations.stream()
                .filter(invitation -> invitation.getEventId().equals(event.getId()))
                .filter(invitation -> "CONFIRMED".equals(invitation.getStatus()))
                .map(InvitationResponse::getUserEmail)
                .distinct()
                .collect(Collectors.toList());
            
            log.info("Trouvé {} participant(s) confirmé(s) pour l'événement {}", 
                    participantEmails.size(), event.getTitle());
            
            if (participantEmails.isEmpty()) {
                log.info("Aucun participant confirmé pour l'événement {}, pas de rappel envoyé", event.getTitle());
                return;
            }
            
            // Créer le message de rappel
            EventReminderMessage reminderMessage = EventReminderMessage.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .eventDescription(event.getDescription())
                .eventDateTime(event.getEventDate())
                .eventLocation(event.getLocation())
                .organizerId(event.getOrganizerId())
                .participantEmails(participantEmails)
                .build();
            
            // Convertir en JSON et publier sur Kafka
            String jsonMessage = objectMapper.writeValueAsString(reminderMessage);
            kafkaTemplate.send(eventReminderTopic, jsonMessage);
            
            log.info("Rappel envoyé sur Kafka pour l'événement '{}' avec {} participant(s)", 
                    event.getTitle(), participantEmails.size());
            
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du rappel pour l'événement {}: {}", 
                     event.getId(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi du rappel", e);
        }
    }
} 