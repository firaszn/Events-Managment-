package com.example.notificationservice.service;

import com.example.notificationservice.model.InvitationNotificationDTO;
import com.example.notificationservice.model.EventReminderMessage;
import com.example.notificationservice.model.WaitlistNotificationMessage;
import com.example.notificationservice.model.WaitlistPromotionMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

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

    @KafkaListener(topics = "${kafka.topics.event-reminder}")
    public void handleEventReminder(String message) {
        try {
            log.info("Réception d'un message de rappel d'événement: {}", message);
            
            EventReminderMessage reminderMessage = objectMapper.readValue(message, EventReminderMessage.class);
            
            // Envoyer l'email de rappel à tous les participants
            for (String participantEmail : reminderMessage.getParticipantEmails()) {
                String subject = String.format("Rappel - %s commence dans 1 heure", reminderMessage.getEventTitle());
                String body = String.format(
                    "Bonjour,\n\n" +
                    "Nous vous rappelons que l'événement \"%s\" commence dans 1 heure.\n\n" +
                    "📅 Date et heure : %s\n" +
                    "📍 Lieu : %s\n\n" +
                    "Description : %s\n\n" +
                    "Nous vous attendons !\n\n" +
                    "Cordialement,\n" +
                    "L'équipe d'organisation",
                    reminderMessage.getEventTitle(),
                    reminderMessage.getEventDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")),
                    reminderMessage.getEventLocation(),
                    reminderMessage.getEventDescription() != null ? reminderMessage.getEventDescription() : "Aucune description"
                );
                
                emailService.sendEmail(participantEmail, subject, body);
                log.info("Email de rappel envoyé à {}", participantEmail);
            }
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement du rappel d'événement: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.waitlist-notification}")
    public void handleWaitlistNotification(String message) {
        try {
            log.info("Réception d'une notification de liste d'attente: {}", message);
            
            WaitlistNotificationMessage waitlistMessage = objectMapper.readValue(message, WaitlistNotificationMessage.class);
            
            String subject = String.format("🎉 Une place s'est libérée pour \"%s\"", waitlistMessage.getEventTitle());
            String body = String.format(
                "Bonne nouvelle !\n\n" +
                "Une place s'est libérée pour l'événement \"%s\" et vous êtes le prochain sur la liste d'attente !\n\n" +
                "📅 Date et heure : %s\n" +
                "📍 Lieu : %s\n" +
                "🔢 Votre position était : %d\n\n" +
                "⏰ IMPORTANT : Vous avez jusqu'au %s pour confirmer votre participation.\n" +
                "Si vous ne confirmez pas dans ce délai, la place sera proposée à la personne suivante.\n\n" +
                "Pour confirmer votre place, connectez-vous à la plateforme et cliquez sur \"Confirmer ma place\".\n\n" +
                "Ne manquez pas cette opportunité !\n\n" +
                "Cordialement,\n" +
                "L'équipe d'organisation",
                waitlistMessage.getEventTitle(),
                waitlistMessage.getEventDate() != null ? 
                    waitlistMessage.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) : "Date à confirmer",
                waitlistMessage.getEventLocation(),
                waitlistMessage.getPosition(),
                waitlistMessage.getExpiresAt() != null ? 
                    waitlistMessage.getExpiresAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) : "24 heures"
            );
            
            emailService.sendEmail(waitlistMessage.getUserEmail(), subject, body);
            log.info("Email de notification de liste d'attente envoyé à {}", waitlistMessage.getUserEmail());
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement de la notification de liste d'attente: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.waitlist-promotion}")
    public void handleWaitlistPromotion(String message) {
        try {
            log.info("Réception d'une notification de promotion depuis la liste d'attente: {}", message);
            
            WaitlistPromotionMessage promotionMessage = objectMapper.readValue(message, WaitlistPromotionMessage.class);
            
            String seatInfo = "";
            if (promotionMessage.getSeatRow() != null && promotionMessage.getSeatNumber() != null) {
                seatInfo = String.format("\n🎫 Votre place : Rangée %d, Siège %d", 
                    promotionMessage.getSeatRow(), promotionMessage.getSeatNumber());
            }
            
            String subject = String.format("🎉 Félicitations ! Vous avez une place pour \"%s\"", promotionMessage.getEventTitle());
            String body = String.format(
                "Bonne nouvelle !\n\n" +
                "Une place s'est libérée pour l'événement \"%s\" et vous avez été automatiquement promu depuis la liste d'attente !\n\n" +
                "📅 Date et heure : %s\n" +
                "📍 Lieu : %s%s\n\n" +
                "✅ Votre inscription est maintenant confirmée.\n\n" +
                "Nous vous attendons avec impatience !\n\n" +
                "Cordialement,\n" +
                "L'équipe d'organisation",
                promotionMessage.getEventTitle(),
                promotionMessage.getEventDate() != null ? 
                    promotionMessage.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) : "Date à confirmer",
                promotionMessage.getEventLocation(),
                seatInfo
            );
            
            emailService.sendEmail(promotionMessage.getUserEmail(), subject, body);
            log.info("Email de confirmation de promotion envoyé à {}", promotionMessage.getUserEmail());
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement de la notification de promotion: {}", e.getMessage(), e);
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

    private String buildReminderEmailContent(EventReminderMessage reminder) {
        StringBuilder content = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
        
        content.append("Bonjour,\n\n");
        content.append("Ceci est un rappel concernant l'événement auquel vous êtes inscrit :\n\n");
        
        content.append("📅 Événement : ").append(reminder.getEventTitle()).append("\n");
        content.append("📍 Lieu : ").append(reminder.getEventLocation()).append("\n");
        content.append("🕐 Date et heure : ").append(reminder.getEventDateTime().format(formatter)).append("\n\n");
        
        if (reminder.getEventDescription() != null && !reminder.getEventDescription().trim().isEmpty()) {
            content.append("📝 Description : ").append(reminder.getEventDescription()).append("\n\n");
        }
        
        content.append("⏰ L'événement commence dans environ 1 heure !\n\n");
        content.append("Nous vous rappelons de :\n");
        content.append("• Arriver 15 minutes avant le début\n");
        content.append("• Apporter une pièce d'identité si nécessaire\n");
        content.append("• Vérifier l'adresse du lieu\n\n");
        
        content.append("Nous avons hâte de vous voir !\n\n");
        content.append("Cordialement,\n");
        content.append("L'équipe événementielle");

        return content.toString();
    }
} 