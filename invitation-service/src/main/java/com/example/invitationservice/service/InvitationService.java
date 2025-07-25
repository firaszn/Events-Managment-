package com.example.invitationservice.service;

import com.example.invitationservice.entity.InvitationEntity;
import com.example.invitationservice.entity.InvitationStatus;
import com.example.invitationservice.client.EventClient;
import com.example.invitationservice.entity.SeatInfo;
import com.example.invitationservice.model.InvitationRequest;
import com.example.invitationservice.model.SeatInfoRequest;
import com.example.invitationservice.repository.InvitationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

class InvitationException extends RuntimeException {
    public InvitationException(String message) { super(message); }
    public InvitationException(String message, Throwable cause) { super(message, cause); }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final EventClient eventClient;

    @Value("${kafka.topics.invitation-responded}")
    private String invitationRespondedTopic;

    @Value("${kafka.topics.waitlist-promotion:waitlist.promotion}")
    private String waitlistPromotionTopic;

    @Transactional(readOnly = true)
    public List<InvitationEntity> getAllInvitations() {
        return invitationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean isUserRegisteredForEvent(Long eventId, String userEmail) {
        // Un utilisateur est considéré comme inscrit seulement si son invitation est confirmée
        return invitationRepository.existsByEventIdAndUserEmailAndStatusConfirmed(eventId, userEmail);
    }

    @Transactional(readOnly = true)
    public boolean hasUserPendingInvitation(Long eventId, String userEmail) {
        // Vérifier si l'utilisateur a une invitation en attente
        return invitationRepository.existsByEventIdAndUserEmailAndStatusPending(eventId, userEmail);
    }

    @Transactional(readOnly = true)
    public boolean isSeatOccupied(Long eventId, SeatInfo seatInfo) {
        return invitationRepository.existsByEventIdAndSeatInfo(eventId, seatInfo);
    }

    @Transactional
    public InvitationEntity createInvitation(InvitationRequest request) {
        // Convertir l'eventId en Long
        Long eventId = Long.parseLong(request.getEventId());
        
        // Vérifier si l'utilisateur est déjà inscrit
        if (invitationRepository.existsByEventIdAndUserEmail(eventId, request.getUserEmail())) {
            throw new InvitationException("Vous êtes déjà inscrit à cet événement");
        }

        // Convertir les informations de siège
        SeatInfo seatInfo = null;
        if (request.getSeatInfo() != null) {
            Integer row = request.getSeatInfo().getRow();
            Integer number = request.getSeatInfo().getNumber();
            if (row == null || number == null) {
                log.error("Tentative de création d'une invitation avec un siège incomplet: row={}, number={}", row, number);
                throw new InvitationException("La place doit comporter un numéro de rangée et de siège.");
            }
            seatInfo = SeatInfo.builder()
                    .row(row)
                    .number(number)
                    .build();

            // Vérifier si le siège est déjà occupé
            if (isSeatOccupied(eventId, seatInfo)) {
                throw new InvitationException("Cette place est déjà occupée");
            }
        }

        // Créer l'invitation
        InvitationEntity invitation = InvitationEntity.builder()
                .eventId(eventId)
                .eventTitle(request.getEventTitle())
                .userEmail(request.getUserEmail())
                .status(InvitationStatus.PENDING) // En attente de confirmation par l'admin
                .seatInfo(seatInfo)
                .build();

        // Sauvegarder l'invitation
        InvitationEntity savedInvitation = invitationRepository.save(invitation);

        log.info("Invitation créée en attente de confirmation pour {} - événement {} - siège {},{}",
                request.getUserEmail(), eventId,
                seatInfo != null ? seatInfo.getRow() : "N/A",
                seatInfo != null ? seatInfo.getNumber() : "N/A");
        
        return savedInvitation;
    }

    @Transactional
    public InvitationEntity confirmInvitation(Long invitationId) {
        InvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationException("Invitation non trouvée"));

        invitation.setStatus(InvitationStatus.CONFIRMED);
        invitation = invitationRepository.save(invitation);

        // Publier le message Kafka pour l'envoi de l'email
        try {
            // Créer un objet pour le message Kafka
            var request = new InvitationRequest();
            request.setEventId(invitation.getEventId().toString());
            request.setEventTitle(invitation.getEventTitle());
            request.setUserEmail(invitation.getUserEmail());
            if (invitation.getSeatInfo() != null) {
                var seatInfoRequest = SeatInfoRequest.builder()
                    .row(invitation.getSeatInfo().getRow())
                    .number(invitation.getSeatInfo().getNumber())
                    .build();
                request.setSeatInfo(seatInfoRequest);
            }

            // Convertir la requête en JSON string
            String jsonMessage = objectMapper.writeValueAsString(request);
            
            // Envoyer le message
            kafkaTemplate.send(invitationRespondedTopic, jsonMessage);
            
            log.info("Message Kafka envoyé pour la confirmation de l'inscription de {} à l'événement {}", 
                    invitation.getUserEmail(), invitation.getEventTitle());
            
            return invitation;
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message Kafka", e);
            throw new InvitationException("Erreur lors de l'envoi de la notification", e);
        }
    }

    @Transactional(readOnly = true)
    public List<SeatInfo> getOccupiedSeatsForEvent(Long eventId) {
        log.info("=== DEBUT getOccupiedSeatsForEvent pour event {} ===", eventId);
        
        // D'abord, récupérer toutes les invitations pour cet événement
        List<InvitationEntity> allInvitations = invitationRepository.findAll().stream()
            .filter(inv -> inv.getEventId().equals(eventId))
            .toList();
        log.info("Total invitations trouvées (toutes) pour l'événement {}: {}", eventId, allInvitations.size());
        
        for (InvitationEntity inv : allInvitations) {
            log.info("Invitation trouvée: id={}, status={}, userEmail={}, seatInfo={}", 
                inv.getId(), inv.getStatus(), inv.getUserEmail(), inv.getSeatInfo());
        }
        
        // Maintenant utiliser la requête filtrée
        List<InvitationEntity> invitations = invitationRepository.findByEventId(eventId);
        log.info("Invitations FILTRÉES (avec sièges et non annulées) pour l'événement {}: {}", eventId, invitations.size());
        
        List<SeatInfo> occupiedSeats = new ArrayList<>();
        for (InvitationEntity invitation : invitations) {
            SeatInfo seatInfo = invitation.getSeatInfo();
            if (seatInfo != null) {
                SeatInfo newSeatInfo = SeatInfo.builder()
                    .row(seatInfo.getRow())
                    .number(seatInfo.getNumber())
                    .build();
                occupiedSeats.add(newSeatInfo);
                log.info("Place réservée ajoutée: row={}, number={} (invitation id={})", 
                    seatInfo.getRow(), seatInfo.getNumber(), invitation.getId());
            } else {
                log.warn("Invitation {} sans informations de siège ignorée", invitation.getId());
            }
        }
        
        log.info("=== FIN getOccupiedSeatsForEvent: {} places réservées retournées ===", occupiedSeats.size());
        return occupiedSeats;
    }

    @Transactional
    public void deleteInvitation(Long invitationId) {
        log.info("Suppression de l'invitation avec l'ID: {}", invitationId);

        InvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationException("Invitation non trouvée avec l'ID: " + invitationId));

        log.info("Suppression de l'invitation pour l'utilisateur {} et l'événement {} (statut: {})",
                invitation.getUserEmail(), invitation.getEventTitle(), invitation.getStatus());

        Long eventId = invitation.getEventId();
        boolean wasConfirmed = invitation.getStatus() == InvitationStatus.CONFIRMED;

        // Libérer la place si elle était occupée
        if (invitation.getSeatInfo() != null && wasConfirmed) {
            log.info("Libération de la place: Rangée {}, Siège {} pour l'événement {}",
                    invitation.getSeatInfo().getRow(), invitation.getSeatInfo().getNumber(), eventId);
        }

        invitationRepository.delete(invitation);

        log.info("Invitation supprimée avec succès");

        // Déclencher la redistribution automatique seulement si c'était une invitation confirmée
        if (wasConfirmed) {
            log.info("Place libérée - déclenchement de la redistribution automatique pour l'événement {}", eventId);
            triggerWaitlistRedistribution(eventId, null);
        } else {
            log.info("Invitation non confirmée - pas de redistribution nécessaire pour l'événement {}", eventId);
        }
    }

    /**
     * Déclenche la redistribution automatique de la liste d'attente
     * Cette méthode envoie un message Kafka pour informer le service d'événements
     */
    private void triggerWaitlistRedistribution(Long eventId, SeatInfo seatInfoLiberee) {
        try {
            String redistribution = "{\"eventId\":" + eventId + ",\"availableSlots\":1";
            if (seatInfoLiberee != null) {
                redistribution += ",\"row\":" + seatInfoLiberee.getRow() + ",\"number\":" + seatInfoLiberee.getNumber();
            }
            redistribution += "}";
            log.info("[TRIGGER REDISTRIBUTION] eventId={}, row={}, number={}", eventId, seatInfoLiberee != null ? seatInfoLiberee.getRow() : null, seatInfoLiberee != null ? seatInfoLiberee.getNumber() : null);
            kafkaTemplate.send("waitlist.redistribution", redistribution);
            log.info("Message de redistribution envoyé pour l'événement {} avec place libérée row={}, number={}", eventId, seatInfoLiberee != null ? seatInfoLiberee.getRow() : null, seatInfoLiberee.getNumber());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message de redistribution pour l'événement {}: {}", 
                     eventId, e.getMessage(), e);
        }
    }

    /**
     * Écouter les messages de création d'invitation depuis la liste d'attente
     */
    @org.springframework.kafka.annotation.KafkaListener(
        topics = "waitlist.invitation.created",
        groupId = "invitation-service-group"
    )
    public void handleWaitlistInvitationCreation(String message) {
        try {
            log.info("=== DEBUT handleWaitlistInvitationCreation ===");
            log.info("Message de création d'invitation WAITLIST reçu: {}", message);

            // Vérifier que le message n'est pas vide
            if (message == null || message.trim().isEmpty()) {
                log.error("Message vide reçu pour création d'invitation WAITLIST");
                return;
            }

            // Parser le message JSON
            var jsonNode = objectMapper.readTree(message);
            Long eventId = jsonNode.get("eventId").asLong();
            String eventTitle = jsonNode.get("eventTitle").asText();
            String userEmail = jsonNode.get("userEmail").asText();

            log.info("Données parsées: eventId={}, eventTitle={}, userEmail={}", eventId, eventTitle, userEmail);

            // Vérifier si une invitation existe déjà pour cet utilisateur et cet événement
            Optional<InvitationEntity> existingInvitation = invitationRepository.findByEventIdAndUserEmail(eventId, userEmail);
            if (existingInvitation.isPresent()) {
                log.warn("Invitation déjà existante pour {} - événement {} avec statut {}",
                        userEmail, eventId, existingInvitation.get().getStatus());
                return;
            }

            // Créer l'invitation avec statut WAITLIST
            InvitationEntity invitation = InvitationEntity.builder()
                    .eventId(eventId)
                    .eventTitle(eventTitle)
                    .userEmail(userEmail)
                    .status(InvitationStatus.WAITLIST) // Statut spécial pour liste d'attente
                    .seatInfo(null) // Pas de siège assigné pour l'instant
                    .build();

            InvitationEntity savedInvitation = invitationRepository.save(invitation);
            log.info("Invitation WAITLIST créée avec succès: ID={}, userEmail={}, eventId={}, statut={}",
                    savedInvitation.getId(), savedInvitation.getUserEmail(), savedInvitation.getEventId(), savedInvitation.getStatus());

            log.info("=== FIN handleWaitlistInvitationCreation ===");

        } catch (Exception e) {
            log.error("=== ERREUR handleWaitlistInvitationCreation ===");
            log.error("Erreur lors de la création de l'invitation WAITLIST: {}", e.getMessage(), e);
            log.error("Message reçu: {}", message);
        }
    }

    /**
     * Écouter les messages de confirmation automatique d'invitation
     */
    @org.springframework.kafka.annotation.KafkaListener(
        topics = "invitation.auto.confirm",
        groupId = "invitation-service-group"
    )
    public void handleAutoConfirmInvitation(String message) {
        try {
            log.info("=== DEBUT handleAutoConfirmInvitation ===");
            log.info("Message de confirmation automatique reçu: {}", message);

            // Parser le message JSON
            var jsonNode = objectMapper.readTree(message);
            Long eventId = jsonNode.get("eventId").asLong();
            String userEmail = jsonNode.get("userEmail").asText();
            Integer row = jsonNode.has("row") && !jsonNode.get("row").isNull() ? jsonNode.get("row").asInt() : null;
            Integer number = jsonNode.has("number") && !jsonNode.get("number").isNull() ? jsonNode.get("number").asInt() : null;

            log.info("Données parsées: eventId={}, userEmail={}, row={}, number={}", eventId, userEmail, row, number);

            // Trouver l'invitation WAITLIST correspondante
            Optional<InvitationEntity> invitationOpt = invitationRepository.findByEventIdAndUserEmail(eventId, userEmail);

            if (invitationOpt.isPresent()) {
                InvitationEntity invitation = invitationOpt.get();
                log.info("Invitation trouvée: ID={}, statut={}", invitation.getId(), invitation.getStatus());

                if (invitation.getStatus() == InvitationStatus.WAITLIST) {
                    // Confirmer automatiquement l'invitation
                    invitation.setStatus(InvitationStatus.CONFIRMED);
                    // Affecter la place libérée si elle existe
                    if (row != null && number != null) {
                        invitation.setSeatInfo(SeatInfo.builder().row(row).number(number).build());
                        log.info("Affectation de la place libérée: row={}, number={}", row, number);
                    }
                    InvitationEntity savedInvitation = invitationRepository.save(invitation);

                    log.info("Invitation WAITLIST confirmée automatiquement: ID={}, nouveau statut={}",
                            savedInvitation.getId(), savedInvitation.getStatus());

                    // Envoyer email de confirmation de promotion depuis la liste d'attente
                    sendWaitlistPromotionEmail(savedInvitation, row, number);
                    log.info("=== FIN handleAutoConfirmInvitation (succès) ===");
                } else {
                    log.warn("Invitation trouvée mais statut incorrect: {} pour {} - événement {}",
                            invitation.getStatus(), userEmail, eventId);
                    log.info("=== FIN handleAutoConfirmInvitation (statut incorrect) ===");
                }
            } else {
                log.warn("Aucune invitation trouvée pour {} - événement {}", userEmail, eventId);
                log.info("=== FIN handleAutoConfirmInvitation (invitation non trouvée) ===");
            }

        } catch (Exception e) {
            log.error("=== ERREUR handleAutoConfirmInvitation ===");
            log.error("Erreur lors de la confirmation automatique d'invitation: {}", e.getMessage(), e);
            log.error("Message reçu: {}", message);
        }
    }

    /**
     * Envoyer email de confirmation
     */
    private void sendConfirmationEmail(InvitationEntity invitation) {
        try {
            // Créer le message de notification
            String notificationMessage = String.format(
                "{\"type\":\"INVITATION_CONFIRMED\",\"userEmail\":\"%s\",\"eventTitle\":\"%s\",\"eventId\":%d}",
                invitation.getUserEmail(), invitation.getEventTitle(), invitation.getEventId()
            );

            kafkaTemplate.send("notification.send", notificationMessage);
            log.info("Email de confirmation envoyé pour {} - événement {}",
                    invitation.getUserEmail(), invitation.getEventId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de confirmation: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoyer email de confirmation de promotion depuis la liste d'attente
     */
    private void sendWaitlistPromotionEmail(InvitationEntity invitation, Integer row, Integer number) {
        try {
            // Récupérer les détails de l'événement
            EventClient.EventDetails eventDetails = null;
            try {
                var response = eventClient.getEventById(invitation.getEventId());
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    eventDetails = response.getBody();
                }
            } catch (Exception e) {
                log.warn("Impossible de récupérer les détails de l'événement {}: {}", invitation.getEventId(), e.getMessage());
            }

            // Créer le message de promotion depuis la liste d'attente
            String promotionMessage = String.format(
                "{\"eventId\":%d,\"eventTitle\":\"%s\",\"eventDate\":\"%s\",\"eventLocation\":\"%s\",\"userEmail\":\"%s\",\"seatRow\":%s,\"seatNumber\":%s,\"promotionDate\":\"%s\"}",
                invitation.getEventId(),
                invitation.getEventTitle(),
                eventDetails != null && eventDetails.getEventDate() != null ? eventDetails.getEventDate() : "",
                eventDetails != null && eventDetails.getLocation() != null ? eventDetails.getLocation() : "",
                invitation.getUserEmail(),
                row != null ? row.toString() : "null",
                number != null ? number.toString() : "null",
                java.time.LocalDateTime.now().toString()
            );

            kafkaTemplate.send(waitlistPromotionTopic, promotionMessage);
            log.info("Email de confirmation de promotion envoyé pour {} - événement {} - place: rangée {}, siège {}",
                    invitation.getUserEmail(), invitation.getEventId(), row, number);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de promotion: {}", e.getMessage(), e);
        }
    }

    /**
     * Annuler l'inscription d'un utilisateur
     */
    @Transactional
    public void cancelUserRegistration(Long eventId, String userEmail) {
        Optional<InvitationEntity> invitationOpt = invitationRepository.findByEventIdAndUserEmail(eventId, userEmail);

        if (invitationOpt.isEmpty()) {
            throw new InvitationException("Aucune invitation trouvée pour cet utilisateur et cet événement");
        }

        InvitationEntity invitation = invitationOpt.get();

        // Vérifier si c'était une invitation confirmée avant de la changer
        boolean wasConfirmed = invitation.getStatus() == InvitationStatus.CONFIRMED;

        // Libérer la place si elle était occupée
        SeatInfo seatInfoLiberee = null;
        if (invitation.getSeatInfo() != null && wasConfirmed) {
            log.info("Libération de la place: Rangée {}, Siège {} pour l'événement {} (annulation par {})",
                    invitation.getSeatInfo().getRow(), invitation.getSeatInfo().getNumber(), eventId, userEmail);
            seatInfoLiberee = invitation.getSeatInfo();
        }

        // Changer le statut à CANCELLED
        invitation.setStatus(InvitationStatus.CANCELLED);
        invitation.setSeatInfo(null); // On libère la place
        invitationRepository.save(invitation);

        log.info("Inscription annulée pour {} - événement {} (statut était: {})",
                userEmail, eventId, wasConfirmed ? "CONFIRMED" : "NON-CONFIRMED");

        // Déclencher la redistribution automatique si c'était une invitation confirmée
        if (wasConfirmed) {
            log.info("Place libérée - déclenchement de la redistribution automatique pour l'événement {}", eventId);
            triggerWaitlistRedistribution(eventId, seatInfoLiberee); // On passe la place libérée
        } else {
            log.info("Invitation non confirmée - pas de redistribution nécessaire pour l'événement {}", eventId);
        }
    }

    /**
     * Vider toutes les invitations (pour admin)
     */
    @Transactional
    public long clearAllInvitations() {
        log.info("=== DEBUT clearAllInvitations ===");

        long count = invitationRepository.count();
        log.info("Nombre d'invitations à supprimer: {}", count);

        invitationRepository.deleteAll();
        log.info("Toutes les invitations supprimées");

        log.info("=== FIN clearAllInvitations ===");
        return count;
    }
}