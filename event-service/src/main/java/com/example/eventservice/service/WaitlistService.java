package com.example.eventservice.service;

import com.example.eventservice.client.InvitationClient;
import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.entity.WaitlistEntry;
import com.example.eventservice.model.InvitationResponse;
import com.example.eventservice.model.WaitlistResponse;
import com.example.eventservice.repository.EventRepository;
import com.example.eventservice.repository.WaitlistRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final EventRepository eventRepository;
    private final InvitationClient invitationClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${waitlist.notification-expiry-hours:24}")
    private int notificationExpiryHours;

    @Value("${waitlist.redistribution-batch-size:5}")
    private int redistributionBatchSize;

    @Value("${kafka.topics.waitlist-notification:waitlist.notification}")
    private String waitlistNotificationTopic;

    /**
     * Ajouter un utilisateur à la liste d'attente
     */
    @Transactional
    public WaitlistResponse joinWaitlist(Long eventId, String userEmail) {
        // Vérifier que l'événement existe et que la liste d'attente est activée
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Événement non trouvé"));

        if (!Boolean.TRUE.equals(event.getWaitlistEnabled())) {
            throw new IllegalStateException("La liste d'attente n'est pas activée pour cet événement");
        }

        // Vérifier que l'utilisateur n'est pas déjà inscrit
        if (isUserRegistered(eventId, userEmail)) {
            throw new IllegalStateException("Vous êtes déjà inscrit à cet événement");
        }

        // Vérifier que l'utilisateur n'est pas déjà en liste d'attente
        Optional<WaitlistEntry> existingEntry = waitlistRepository.findByEventIdAndUserEmail(eventId, userEmail);
        if (existingEntry.isPresent()) {
            WaitlistEntry entry = existingEntry.get();
            if (entry.getStatus() == WaitlistEntry.WaitlistStatus.WAITING) {
                return toResponse(entry, event.getTitle());
            } else {
                throw new IllegalStateException("Vous êtes déjà en liste d'attente pour cet événement");
            }
        }

        // Ajouter à la liste d'attente
        int nextPosition = waitlistRepository.getNextPosition(eventId);
        WaitlistEntry newEntry = WaitlistEntry.builder()
                .eventId(eventId)
                .userEmail(userEmail)
                .position(nextPosition)
                .status(WaitlistEntry.WaitlistStatus.WAITING)
                .notificationSent(false)
                .build();

        WaitlistEntry savedEntry = waitlistRepository.save(newEntry);
        log.info("Utilisateur {} ajouté à la liste d'attente de l'événement {} en position {}",
                userEmail, eventId, nextPosition);

        // Créer automatiquement une invitation WAITLIST
        try {
            log.info("=== DEBUT création invitation WAITLIST ===");
            WaitlistInvitationMessage message = WaitlistInvitationMessage.builder()
                    .eventId(eventId)
                    .eventTitle(event.getTitle())
                    .userEmail(userEmail)
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(message);
            log.info("Message JSON créé: {}", jsonMessage);
            log.info("Envoi du message vers le topic: waitlist.invitation.created");

            kafkaTemplate.send("waitlist.invitation.created", jsonMessage);
            log.info("Message Kafka envoyé avec succès pour {} - événement {}", userEmail, eventId);
            log.info("=== FIN création invitation WAITLIST ===");
        } catch (Exception e) {
            log.error("=== ERREUR création invitation WAITLIST ===");
            log.error("Erreur lors de la création de l'invitation WAITLIST pour {} - événement {}: {}",
                     userEmail, eventId, e.getMessage(), e);
        }

        return toResponse(savedEntry, event.getTitle());
    }

    /**
     * Quitter la liste d'attente
     */
    @Transactional
    public void leaveWaitlist(Long eventId, String userEmail) {
        WaitlistEntry entry = waitlistRepository.findByEventIdAndUserEmail(eventId, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Vous n'êtes pas en liste d'attente pour cet événement"));

        int deletedPosition = entry.getPosition();
        waitlistRepository.delete(entry);

        // Mettre à jour les positions des autres utilisateurs
        waitlistRepository.updatePositionsAfterDeletion(eventId, deletedPosition);

        log.info("Utilisateur {} retiré de la liste d'attente de l'événement {}", userEmail, eventId);
    }

    /**
     * Redistribuer les places quand un participant annule
     */
    @Transactional
    public void redistributeAvailableSlots(Long eventId, int availableSlots) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Événement non trouvé"));

        if (!Boolean.TRUE.equals(event.getWaitlistEnabled())) {
            log.debug("Liste d'attente désactivée pour l'événement {}", eventId);
            return;
        }

        // Limiter le nombre de places redistribuées
        int slotsToRedistribute = Math.min(availableSlots, redistributionBatchSize);

        List<WaitlistEntry> waitingEntries = waitlistRepository.findWaitingByEventId(eventId);
        int assignmentsToMake = Math.min(slotsToRedistribute, waitingEntries.size());

        if (assignmentsToMake == 0) {
            log.info("Aucune personne en attente pour l'événement {}", eventId);
            return;
        }

        for (int i = 0; i < assignmentsToMake; i++) {
            WaitlistEntry entry = waitingEntries.get(i);

            log.info("=== DEBUT assignation automatique pour {} ===", entry.getUserEmail());

            // Confirmer automatiquement la place
            entry.setStatus(WaitlistEntry.WaitlistStatus.CONFIRMED);
            entry.setNotificationSent(true);
            waitlistRepository.save(entry);

            log.info("Statut liste d'attente mis à jour: {} -> CONFIRMED", entry.getUserEmail());

            // Confirmer automatiquement l'invitation WAITLIST correspondante
            try {
                AutoConfirmInvitationMessage message = AutoConfirmInvitationMessage.builder()
                        .eventId(eventId)
                        .userEmail(entry.getUserEmail())
                        .build();

                String jsonMessage = objectMapper.writeValueAsString(message);
                log.info("Message de confirmation automatique créé: {}", jsonMessage);
                log.info("Envoi vers topic: invitation.auto.confirm");

                kafkaTemplate.send("invitation.auto.confirm", jsonMessage);
                log.info("Confirmation automatique déclenchée pour {} - événement {}", entry.getUserEmail(), eventId);
                log.info("=== FIN assignation automatique pour {} ===", entry.getUserEmail());
            } catch (Exception e) {
                log.error("=== ERREUR assignation automatique pour {} ===", entry.getUserEmail());
                log.error("Erreur lors de la confirmation automatique pour {} - événement {}: {}",
                         entry.getUserEmail(), eventId, e.getMessage(), e);
            }
        }

        log.info("Redistribution automatique de {} place(s) pour l'événement {} - {} assignation(s) effectuée(s)",
                slotsToRedistribute, eventId, assignmentsToMake);
    }

    /**
     * Confirmer une place depuis la liste d'attente
     */
    @Transactional
    public void confirmWaitlistSpot(Long eventId, String userEmail) {
        WaitlistEntry entry = waitlistRepository.findByEventIdAndUserEmail(eventId, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Entrée en liste d'attente non trouvée"));

        if (entry.getStatus() != WaitlistEntry.WaitlistStatus.NOTIFIED) {
            throw new IllegalStateException("Cette entrée ne peut pas être confirmée");
        }

        if (entry.getExpiresAt() != null && entry.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Le délai de confirmation a expiré");
        }

        // Récupérer les informations de l'événement
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Événement non trouvé"));

        // Marquer l'entrée comme confirmée
        entry.setStatus(WaitlistEntry.WaitlistStatus.CONFIRMED);
        waitlistRepository.save(entry);

        // Créer automatiquement une invitation avec statut WAITLIST
        try {
            // Créer une invitation spéciale pour les utilisateurs de liste d'attente
            // Cette invitation aura le statut WAITLIST et sera visible par l'admin
            WaitlistInvitationMessage message = WaitlistInvitationMessage.builder()
                    .eventId(eventId)
                    .eventTitle(event.getTitle())
                    .userEmail(userEmail)
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("waitlist.invitation.created", jsonMessage);
            log.info("Invitation WAITLIST créée pour {} - événement {}", userEmail, eventId);
        } catch (Exception e) {
            log.error("Erreur lors de la création de l'invitation WAITLIST pour {} - événement {}: {}",
                     userEmail, eventId, e.getMessage(), e);
            // Ne pas faire échouer la confirmation pour cette erreur
        }

        log.info("Place confirmée depuis la liste d'attente pour {} - événement {}", userEmail, eventId);
    }

    /**
     * Traitement automatique des notifications expirées
     */
    @Scheduled(fixedRate = 300000) // Toutes les 5 minutes
    @Transactional
    public void processExpiredNotifications() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<WaitlistEntry> expiredEntries = waitlistRepository.findExpiredNotifications(now);

            if (expiredEntries.isEmpty()) {
                return;
            }

            log.info("Traitement de {} notification(s) expirée(s)", expiredEntries.size());

            for (WaitlistEntry entry : expiredEntries) {
                entry.setStatus(WaitlistEntry.WaitlistStatus.EXPIRED);
                waitlistRepository.save(entry);
                log.info("Notification expirée pour {} - événement {}", entry.getUserEmail(), entry.getEventId());
            }

            // Relancer la redistribution pour les événements affectés
            expiredEntries.stream()
                    .map(WaitlistEntry::getEventId)
                    .distinct()
                    .forEach(eventId -> redistributeAvailableSlots(eventId, 1));

        } catch (Exception e) {
            log.error("Erreur lors du traitement des notifications expirées", e);
        }
    }

    /**
     * Obtenir la position d'un utilisateur dans la liste d'attente
     */
    public Optional<WaitlistResponse> getUserWaitlistPosition(Long eventId, String userEmail) {
        return waitlistRepository.findByEventIdAndUserEmail(eventId, userEmail)
                .map(entry -> {
                    EventEntity event = eventRepository.findById(eventId).orElse(null);
                    String eventTitle = event != null ? event.getTitle() : null;
                    return toResponse(entry, eventTitle);
                });
    }

    /**
     * Obtenir le nombre de personnes en attente
     */
    public long getWaitlistCount(Long eventId) {
        return waitlistRepository.countWaitingByEventId(eventId);
    }

    /**
     * Vérifier si l'événement est complet
     */
    public boolean isEventFull(Long eventId) {
        EventEntity event = eventRepository.findById(eventId).orElse(null);
        if (event == null || event.getMaxCapacity() == null) {
            return false;
        }

        long confirmedParticipants = getConfirmedParticipantsCount(eventId);
        return confirmedParticipants >= event.getMaxCapacity();
    }

    /**
     * Obtenir le nombre de participants confirmés
     */
    public long getConfirmedParticipantsCount(Long eventId) {
        try {
            List<InvitationResponse> invitations = invitationClient.getAllInvitations().getBody();
            if (invitations == null) {
                log.warn("Aucune invitation retournée pour le comptage des participants de l'événement {}", eventId);
                return 0;
            }
            // Log toutes les invitations pour debug
            long totalForEvent = invitations.stream()
                .filter(inv -> String.valueOf(inv.getEventId()).equals(String.valueOf(eventId)))
                .count();
            log.info("Invitations trouvées pour eventId {}: {}", eventId, totalForEvent);

            // Compter par statut pour debug
            Map<String, Long> statusCounts = invitations.stream()
                .filter(inv -> String.valueOf(inv.getEventId()).equals(String.valueOf(eventId)))
                .collect(Collectors.groupingBy(
                    inv -> inv.getStatus() != null ? inv.getStatus() : "NULL",
                    Collectors.counting()
                ));
            log.info("Répartition des statuts pour l'événement {}: {}", eventId, statusCounts);

            long count = invitations.stream()
                .filter(inv -> String.valueOf(inv.getEventId()).equals(String.valueOf(eventId)))
                .filter(inv -> {
                    String status = inv.getStatus();
                    return status != null && status.equalsIgnoreCase("CONFIRMED");
                })
                .count();
            log.info("Participants confirmés comptés pour l'événement {}: {} (seulement CONFIRMED, excluant CANCELLED)", eventId, count);
            return count;
        } catch (Exception e) {
            log.error("Erreur lors du comptage des participants pour l'événement {}", eventId, e);
            return 0;
        }
    }
    
    /**
     * Méthode utilitaire pour récupérer un événement
     */
    private Optional<com.example.eventservice.entity.EventEntity> getEventById(Long eventId) {
        return eventRepository.findById(eventId);
    }

    private boolean isUserRegistered(Long eventId, String userEmail) {
        try {
            boolean isRegistered = Boolean.TRUE.equals(invitationClient.isUserRegisteredForEvent(eventId, userEmail).getBody());
            log.debug("Vérification inscription: utilisateur {} pour événement {} = {}", userEmail, eventId, isRegistered);
            return isRegistered;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de l'inscription pour {} - événement {}: {}", userEmail, eventId, e.getMessage(), e);
            return false;
        }
    }

    private void sendWaitlistNotification(WaitlistEntry entry, EventEntity event) {
        try {
            WaitlistNotificationMessage message = WaitlistNotificationMessage.builder()
                    .eventId(entry.getEventId())
                    .eventTitle(event.getTitle())
                    .eventDate(event.getEventDate())
                    .eventLocation(event.getLocation())
                    .userEmail(entry.getUserEmail())
                    .position(entry.getPosition())
                    .expiresAt(entry.getExpiresAt())
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(waitlistNotificationTopic, jsonMessage);
            
            log.info("Notification de liste d'attente envoyée pour {} - événement {}", 
                    entry.getUserEmail(), entry.getEventId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification de liste d'attente", e);
        }
    }

    private WaitlistResponse toResponse(WaitlistEntry entry, String eventTitle) {
        return WaitlistResponse.builder()
                .id(entry.getId())
                .eventId(entry.getEventId())
                .eventTitle(eventTitle)
                .userEmail(entry.getUserEmail())
                .position(entry.getPosition())
                .status(entry.getStatus().name())
                .notificationSent(entry.getNotificationSent())
                .expiresAt(entry.getExpiresAt())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WaitlistNotificationMessage {
        private Long eventId;
        private String eventTitle;
        private LocalDateTime eventDate;
        private String eventLocation;
        private String userEmail;
        private Integer position;
        private LocalDateTime expiresAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WaitlistInvitationMessage {
        private Long eventId;
        private String eventTitle;
        private String userEmail;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AutoConfirmInvitationMessage {
        private Long eventId;
        private String userEmail;
        private Integer row;
        private Integer number;
    }

    /**
     * Écoute les messages de redistribution automatique
     */
    @org.springframework.kafka.annotation.KafkaListener(
        topics = "waitlist.redistribution",
        groupId = "event-service-group"
    )
    public void handleWaitlistRedistribution(String message) {
        try {
            log.info("[REDIST] Message de redistribution reçu: {}", message);
            // Parsing JSON robuste
            var jsonNode = objectMapper.readTree(message);
            Long eventId = jsonNode.get("eventId").asLong();
            int availableSlots = jsonNode.get("availableSlots").asInt();
            Integer row = jsonNode.has("row") && !jsonNode.get("row").isNull() ? jsonNode.get("row").asInt() : null;
            Integer number = jsonNode.has("number") && !jsonNode.get("number").isNull() ? jsonNode.get("number").asInt() : null;
            log.info("[REDIST] Extraction row={}, number={}", row, number);
            if (eventId != null) {
                log.info("Déclenchement de la redistribution automatique pour l'événement {} avec {} place(s) row={}, number={}",
                        eventId, availableSlots, row, number);
                redistributeAvailableSlots(eventId, availableSlots, row, number);
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement de la redistribution automatique: {}", e.getMessage(), e);
        }
    }

    // Nouvelle signature
    public void redistributeAvailableSlots(Long eventId, int availableSlots, Integer row, Integer number) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Événement non trouvé"));

        if (!Boolean.TRUE.equals(event.getWaitlistEnabled())) {
            log.debug("Liste d'attente désactivée pour l'événement {}", eventId);
            return;
        }

        int slotsToRedistribute = Math.min(availableSlots, redistributionBatchSize);
        List<WaitlistEntry> waitingEntries = waitlistRepository.findWaitingByEventId(eventId);
        int assignmentsToMake = Math.min(slotsToRedistribute, waitingEntries.size());
        if (assignmentsToMake == 0) {
            log.info("Aucune personne en attente pour l'événement {}", eventId);
            return;
        }
        for (int i = 0; i < assignmentsToMake; i++) {
            WaitlistEntry entry = waitingEntries.get(i);
            log.info("=== DEBUT assignation automatique pour {} ===", entry.getUserEmail());
            entry.setStatus(WaitlistEntry.WaitlistStatus.CONFIRMED);
            entry.setNotificationSent(true);
            waitlistRepository.save(entry);
            log.info("Statut liste d'attente mis à jour: {} -> CONFIRMED", entry.getUserEmail());
            try {
                Integer assignedRow = (i == 0) ? row : null;
                Integer assignedNumber = (i == 0) ? number : null;
                log.info("[REDIST] Affectation à {}: row={}, number={}", entry.getUserEmail(), assignedRow, assignedNumber);
                AutoConfirmInvitationMessage message = AutoConfirmInvitationMessage.builder()
                        .eventId(eventId)
                        .userEmail(entry.getUserEmail())
                        .row(assignedRow)
                        .number(assignedNumber)
                        .build();
                String jsonMessage = objectMapper.writeValueAsString(message);
                log.info("Message de confirmation automatique créé: {}", jsonMessage);
                log.info("Envoi vers topic: invitation.auto.confirm");
                kafkaTemplate.send("invitation.auto.confirm", jsonMessage);
                log.info("Confirmation automatique déclenchée pour {} - événement {}", entry.getUserEmail(), eventId);
                log.info("=== FIN assignation automatique pour {} ===", entry.getUserEmail());
            } catch (Exception e) {
                log.error("=== ERREUR assignation automatique pour {} ===", entry.getUserEmail());
                log.error("Erreur lors de la confirmation automatique pour {} - événement {}: {}",
                         entry.getUserEmail(), eventId, e.getMessage(), e);
            }
        }
        log.info("Redistribution automatique de {} place(s) pour l'événement {} - {} assignation(s) effectuée(s)",
                slotsToRedistribute, eventId, assignmentsToMake);
    }

    /**
     * Écoute les messages de vidage de toutes les listes d'attente
     */
    @org.springframework.kafka.annotation.KafkaListener(
        topics = "waitlist.clear.all",
        groupId = "event-service-group"
    )
    public void handleClearAllWaitlists(String message) {
        try {
            log.info("=== DEBUT handleClearAllWaitlists ===");
            log.info("Message de vidage des listes d'attente reçu: {}", message);

            // Vider toutes les listes d'attente
            long countBefore = waitlistRepository.count();
            waitlistRepository.deleteAll();
            log.info("Nombre d'entrées de liste d'attente supprimées: {}", countBefore);

            log.info("=== FIN handleClearAllWaitlists ===");

        } catch (Exception e) {
            log.error("=== ERREUR handleClearAllWaitlists ===");
            log.error("Erreur lors du vidage des listes d'attente: {}", e.getMessage(), e);
        }
    }

    /**
     * Parse l'eventId depuis le message de redistribution
     */
    private Long parseEventIdFromRedistributionMessage(String message) {
        try {
            // Simple parsing pour {"eventId":123,"availableSlots":1}
            int eventIdStart = message.indexOf("\"eventId\":") + 10;
            int eventIdEnd = message.indexOf(",", eventIdStart);
            if (eventIdEnd == -1) {
                eventIdEnd = message.indexOf("}", eventIdStart);
            }
            return Long.parseLong(message.substring(eventIdStart, eventIdEnd));
        } catch (Exception e) {
            log.error("Erreur lors du parsing de l'eventId: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse availableSlots depuis le message de redistribution
     */
    private int parseAvailableSlotsFromRedistributionMessage(String message) {
        try {
            // Simple parsing pour {"eventId":123,"availableSlots":1}
            int slotsStart = message.indexOf("\"availableSlots\":") + 17;
            int slotsEnd = message.indexOf("}", slotsStart);
            return Integer.parseInt(message.substring(slotsStart, slotsEnd));
        } catch (Exception e) {
            log.error("Erreur lors du parsing des availableSlots: {}", e.getMessage());
            return 1; // Valeur par défaut
        }
    }
} 