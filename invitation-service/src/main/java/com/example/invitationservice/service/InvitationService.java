package com.example.invitationservice.service;

import com.example.invitationservice.entity.InvitationEntity;
import com.example.invitationservice.entity.InvitationStatus;
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

    @Value("${kafka.topics.invitation-responded}")
    private String invitationRespondedTopic;

    @Transactional(readOnly = true)
    public List<InvitationEntity> getAllInvitations() {
        return invitationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean isUserRegisteredForEvent(Long eventId, String userEmail) {
        return invitationRepository.existsByEventIdAndUserEmail(eventId, userEmail);
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
            seatInfo = SeatInfo.builder()
                    .row(request.getSeatInfo().getRow())
                    .number(request.getSeatInfo().getNumber())
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
                .status(InvitationStatus.PENDING)
                .seatInfo(seatInfo)
                .build();

        // Sauvegarder l'invitation
        return invitationRepository.save(invitation);
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
        return invitationRepository.findByEventId(eventId)
                .stream()
                .map(InvitationEntity::getSeatInfo)
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public void deleteInvitation(Long invitationId) {
        log.info("Suppression de l'invitation avec l'ID: {}", invitationId);

        InvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationException("Invitation non trouvée avec l'ID: " + invitationId));

        log.info("Suppression de l'invitation pour l'utilisateur {} et l'événement {}",
                invitation.getUserEmail(), invitation.getEventTitle());

        invitationRepository.delete(invitation);

        log.info("Invitation supprimée avec succès");
    }
}