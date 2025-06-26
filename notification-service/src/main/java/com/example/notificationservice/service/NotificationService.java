package com.example.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final EmailService emailService;

    @KafkaListener(topics = "${kafka.topics.invitation-responded}", groupId = "notification-group")
    public void handleInvitationResponse(String message) {
        try {
            Map<String, Object> messageMap = objectMapper.readValue(message, Map.class);
            
            String userEmail = String.valueOf(messageMap.get("userEmail"));
            String eventTitle = String.valueOf(messageMap.get("eventTitle"));
            Long eventId = Long.valueOf(messageMap.get("eventId").toString());
            String timestamp = LocalDateTime.now().format(formatter);

            // Afficher la notification dans la console
            log.info("╔═══════════════════════════════════════════════");
            log.info("║ 📩 NOTIFICATION D'INSCRIPTION");
            log.info("║ ⏰ Date : {}", timestamp);
            log.info("║ 👤 Destinataire : {}", userEmail);
            log.info("║ 📅 Événement : {}", eventTitle);
            log.info("║ 🔢 ID Événement : {}", eventId);
            log.info("║ ✅ Statut : Inscription confirmée");
            log.info("╚═══════════════════════════════════════════════");

            // Envoyer l'email de confirmation
            emailService.sendEventRegistrationEmail(userEmail, eventTitle);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement de la notification : {}", message, e);
        }
    }
} 