package com.example.invitationservice.service;

import com.example.invitationservice.entity.InvitationEntity;
import com.example.invitationservice.entity.InvitationStatus;
import com.example.invitationservice.entity.SeatInfo;
import com.example.invitationservice.model.InvitationRequest;
import com.example.invitationservice.repository.InvitationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
            throw new RuntimeException("Vous êtes déjà inscrit à cet événement");
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
                throw new RuntimeException("Cette place est déjà occupée");
            }
        }

        // Créer l'invitation
        InvitationEntity invitation = InvitationEntity.builder()
                .eventId(eventId)
                .eventTitle(request.getEventTitle())
                .userEmail(request.getUserEmail())
                .status(InvitationStatus.CONFIRMED)
                .seatInfo(seatInfo)
                .build();

        // Sauvegarder l'invitation
        invitation = invitationRepository.save(invitation);

        // Publier le message Kafka
        try {
            // Convertir la requête en JSON string
            String jsonMessage = objectMapper.writeValueAsString(request);
            
            // Envoyer le message
            kafkaTemplate.send(invitationRespondedTopic, jsonMessage);
            
            log.info("Message Kafka envoyé pour l'inscription de {} à l'événement {} - Place : Rangée {}, Numéro {}", 
                    request.getUserEmail(), request.getEventTitle(), 
                    seatInfo != null ? seatInfo.getRow() : "N/A", 
                    seatInfo != null ? seatInfo.getNumber() : "N/A");
            
            return invitation;
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message Kafka", e);
            throw new RuntimeException("Erreur lors de l'envoi de la notification");
        }
    }

    @Transactional(readOnly = true)
    public List<SeatInfo> getOccupiedSeatsForEvent(Long eventId) {
        return invitationRepository.findByEventId(eventId)
                .stream()
                .map(InvitationEntity::getSeatInfo)
                .filter(seatInfo -> seatInfo != null)
                .collect(Collectors.toList());
    }
} 