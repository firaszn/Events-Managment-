package com.example.invitationservice.service;

import com.example.invitationservice.entity.InvitationEntity;
import com.example.invitationservice.entity.InvitationStatus;
import com.example.invitationservice.model.InvitationRequest;
import com.example.invitationservice.repository.InvitationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kafka.topics.invitation-responded}")
    private String invitationRespondedTopic;

    @Transactional(readOnly = true)
    public boolean isUserRegisteredForEvent(Long eventId, String userEmail) {
        log.debug("Checking database for registration: eventId={}, userEmail={}", eventId, userEmail);
        boolean exists = invitationRepository.existsByEventIdAndUserEmail(eventId, userEmail);
        log.debug("Database check result: {}", exists);
        return exists;
    }

    @Transactional
    public InvitationEntity createInvitation(InvitationRequest request) {
        // Convertir l'eventId en Long
        Long eventId = Long.parseLong(request.getEventId());
        
        // Vérifier si l'utilisateur est déjà inscrit
        if (invitationRepository.existsByEventIdAndUserEmail(eventId, request.getUserEmail())) {
            throw new RuntimeException("Vous êtes déjà inscrit à cet événement");
        }

        // Créer l'invitation
        InvitationEntity invitation = InvitationEntity.builder()
                .eventId(eventId)
                .eventTitle(request.getEventTitle())
                .userEmail(request.getUserEmail())
                .status(InvitationStatus.CONFIRMED)
                .build();

        // Sauvegarder l'invitation
        invitation = invitationRepository.save(invitation);

        // Publier le message Kafka
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("eventId", eventId);
            message.put("userEmail", request.getUserEmail());
            message.put("eventTitle", request.getEventTitle());

            kafkaTemplate.send(invitationRespondedTopic, message);
            log.info("Message Kafka envoyé pour l'inscription de {} à l'événement {}", 
                    request.getUserEmail(), request.getEventTitle());
            
            return invitation;
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message Kafka", e);
            throw new RuntimeException("Erreur lors de l'envoi de la notification");
        }
    }

    @Transactional
    public List<InvitationEntity> getAllInvitations() {
        return invitationRepository.findAll();
    }
} 