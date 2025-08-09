package com.example.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueService {

    private final ObjectMapper objectMapper;
    private final Map<String, Integer> failureCounts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastFailureTimes = new ConcurrentHashMap<>();

    /**
     * Consommer les messages de la dead letter queue
     */
    @KafkaListener(
        topics = "notification.dlq",
        groupId = "notification-dlq-group",
        containerFactory = "dlqKafkaListenerContainerFactory"
    )
    public void handleDeadLetterMessage(String message) {
        try {
            log.warn("Message reçu dans la dead letter queue: {}", message);
            
            // Parser le message de la DLQ
            DeadLetterMessage dlqMessage = parseDeadLetterMessage(message);
            
            // Incrémenter le compteur d'échecs
            String key = dlqMessage.getOriginalTopic() + ":" + dlqMessage.getError();
            failureCounts.merge(key, 1, Integer::sum);
            lastFailureTimes.put(key, LocalDateTime.now());
            
            // Log détaillé de l'échec
            log.error("Échec de traitement pour le topic '{}': {}", 
                dlqMessage.getOriginalTopic(), dlqMessage.getError());
            
            // Optionnel : Tentative de reprocessing
            attemptReprocessing(dlqMessage);
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement du message de la dead letter queue", e);
        }
    }

    /**
     * Tenter de retraiter un message en échec
     */
    private void attemptReprocessing(DeadLetterMessage dlqMessage) {
        try {
            String key = dlqMessage.getOriginalTopic() + ":" + dlqMessage.getError();
            Integer failureCount = failureCounts.get(key);
            
            // Limiter les tentatives de reprocessing
            if (failureCount != null && failureCount > 3) {
                log.error("Trop d'échecs pour le topic '{}', abandon du reprocessing", dlqMessage.getOriginalTopic());
                return;
            }
            
            // Log de la tentative de reprocessing
            log.info("Tentative de reprocessing pour le topic '{}' (échec #{})", 
                dlqMessage.getOriginalTopic(), failureCount);
            
            // Ici, vous pourriez implémenter la logique de reprocessing
            // Par exemple, renvoyer le message vers le topic original
            
        } catch (Exception e) {
            log.error("Erreur lors du reprocessing du message", e);
        }
    }

    /**
     * Obtenir les statistiques des échecs
     */
    public Map<String, Object> getFailureStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        failureCounts.forEach((key, count) -> {
            Map<String, Object> failureStats = new ConcurrentHashMap<>();
            failureStats.put("failureCount", count);
            failureStats.put("lastFailureTime", lastFailureTimes.get(key));
            stats.put(key, failureStats);
        });
        
        return stats;
    }

    /**
     * Nettoyer les anciens échecs
     */
    public void cleanupOldFailures() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        
        lastFailureTimes.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(cutoff)) {
                failureCounts.remove(entry.getKey());
                return true;
            }
            return false;
        });
        
        log.info("Nettoyage des anciens échecs terminé");
    }

    /**
     * Parser le message de la dead letter queue
     */
    private DeadLetterMessage parseDeadLetterMessage(String message) {
        try {
            return objectMapper.readValue(message, DeadLetterMessage.class);
        } catch (Exception e) {
            log.error("Erreur lors du parsing du message DLQ", e);
            // Retourner un message par défaut
            return DeadLetterMessage.builder()
                .originalTopic("unknown")
                .originalMessage(message)
                .error("Parse error: " + e.getMessage())
                .timestamp(LocalDateTime.now().toString())
                .build();
        }
    }

    /**
     * Classe pour représenter un message de la dead letter queue
     */
    public static class DeadLetterMessage {
        private String originalTopic;
        private String originalMessage;
        private String error;
        private String timestamp;

        // Constructeurs, getters, setters
        public DeadLetterMessage() {}

        public DeadLetterMessage(String originalTopic, String originalMessage, String error, String timestamp) {
            this.originalTopic = originalTopic;
            this.originalMessage = originalMessage;
            this.error = error;
            this.timestamp = timestamp;
        }

        public static DeadLetterMessageBuilder builder() {
            return new DeadLetterMessageBuilder();
        }

        public String getOriginalTopic() { return originalTopic; }
        public void setOriginalTopic(String originalTopic) { this.originalTopic = originalTopic; }

        public String getOriginalMessage() { return originalMessage; }
        public void setOriginalMessage(String originalMessage) { this.originalMessage = originalMessage; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public static class DeadLetterMessageBuilder {
            private String originalTopic;
            private String originalMessage;
            private String error;
            private String timestamp;

            DeadLetterMessageBuilder() {}

            public DeadLetterMessageBuilder originalTopic(String originalTopic) {
                this.originalTopic = originalTopic;
                return this;
            }

            public DeadLetterMessageBuilder originalMessage(String originalMessage) {
                this.originalMessage = originalMessage;
                return this;
            }

            public DeadLetterMessageBuilder error(String error) {
                this.error = error;
                return this;
            }

            public DeadLetterMessageBuilder timestamp(String timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public DeadLetterMessage build() {
                return new DeadLetterMessage(originalTopic, originalMessage, error, timestamp);
            }
        }
    }
} 