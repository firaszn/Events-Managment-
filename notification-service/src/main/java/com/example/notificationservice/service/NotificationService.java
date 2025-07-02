package com.example.notificationservice.service;

import com.example.notificationservice.model.InvitationNotificationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.invitation-responded}")
    public void handleInvitationResponse(String message) {
        try {
            // Désérialiser le message JSON en DTO
            InvitationNotificationDTO notification = objectMapper.readValue(message, InvitationNotificationDTO.class);
            
            log.info("Notification reçue pour l'invitation : {} - {}", 
                    notification.getEventTitle(), notification.getUserEmail());

            // Construire le contenu de l'email
            String emailContent = buildEmailContent(notification);

            // Envoyer l'email de confirmation
            emailService.sendEmail(
                notification.getUserEmail(),
                "Confirmation de votre inscription à " + notification.getEventTitle(),
                emailContent
            );

        } catch (Exception e) {
            log.error("Erreur lors du traitement de la notification", e);
        }
    }

    private String buildEmailContent(InvitationNotificationDTO notification) {
        StringBuilder content = new StringBuilder();
        content.append("Bonjour,\n\n");
        content.append("Votre inscription à l'événement '")
              .append(notification.getEventTitle())
              .append("' a été confirmée.\n\n");

        if (notification.getSeatInfo() != null) {
            content.append("Votre place : \n");
            content.append("Rangée : ").append(notification.getSeatInfo().getRow()).append("\n");
            content.append("Numéro : ").append(notification.getSeatInfo().getNumber()).append("\n\n");
        }

        content.append("Nous avons hâte de vous y voir !\n\n");
        content.append("Cordialement,\n");
        content.append("L'équipe événementielle");

        return content.toString();
    }
} 