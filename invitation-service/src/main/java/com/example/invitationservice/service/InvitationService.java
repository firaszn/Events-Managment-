package com.example.invitationservice.service;

import com.example.invitationservice.entity.InvitationEntity;
import com.example.invitationservice.entity.InvitationStatus;
import com.example.invitationservice.model.InvitationRequest;
import com.example.invitationservice.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.invitation-responded}")
    private String invitationRespondedTopic;

    @Transactional
    public InvitationEntity createInvitation(InvitationRequest request) {
        // Vérifier si l'utilisateur est déjà inscrit
        if (invitationRepository.existsByEventIdAndUserEmail(request.getEventId(), request.getUserEmail())) {
            throw new RuntimeException("Vous êtes déjà inscrit à cet événement");
        }

        // Créer l'invitation
        InvitationEntity invitation = InvitationEntity.builder()
                .eventId(request.getEventId())
                .eventTitle(request.getEventTitle())
                .userEmail(request.getUserEmail())
                .status(InvitationStatus.CONFIRMED)
                .build();

        // Sauvegarder l'invitation
        InvitationEntity savedInvitation = invitationRepository.save(invitation);

        // Publier l'événement Kafka
        Map<String, Object> message = new HashMap<>();
        message.put("eventId", savedInvitation.getEventId());
        message.put("userEmail", savedInvitation.getUserEmail());
        message.put("eventTitle", savedInvitation.getEventTitle());

        kafkaTemplate.send(invitationRespondedTopic, message);

        return savedInvitation;
    }
} 