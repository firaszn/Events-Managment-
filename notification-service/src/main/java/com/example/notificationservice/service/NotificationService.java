package com.example.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    @KafkaListener(topics = "${kafka.topics.invitation-responded}", groupId = "notification-group")
    public void handleInvitationResponse(Map<String, Object> message) {
        String userEmail = (String) message.get("userEmail");
        String eventTitle = (String) message.get("eventTitle");

        // Pour l'instant, on log dans la console
        log.info("📩 Notification envoyée à {} :", userEmail);
        log.info("Vous êtes inscrit à l'événement \"{}\".", eventTitle);

        // TODO: Implémenter l'envoi d'email via SMTP plus tard
    }
} 