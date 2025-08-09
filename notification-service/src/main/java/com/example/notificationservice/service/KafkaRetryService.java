package com.example.notificationservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaRetryService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Envoi de message Kafka avec retry et circuit breaker
     */
    @Retry(name = "kafkaRetry", fallbackMethod = "kafkaRetryFallback")
    @CircuitBreaker(name = "kafkaCircuitBreaker", fallbackMethod = "kafkaCircuitBreakerFallback")
    @Bulkhead(name = "kafkaBulkhead", fallbackMethod = "kafkaBulkheadFallback")
    @TimeLimiter(name = "kafkaTimeLimiter", fallbackMethod = "kafkaTimeLimiterFallback")
    public CompletableFuture<SendResult<String, String>> sendMessageWithRetry(String topic, String message) {
        log.info("Tentative d'envoi de message vers le topic: {}", topic);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                SendResult<String, String> result = kafkaTemplate.send(topic, message).get();
                log.info("Message envoyé avec succès vers le topic: {} - Partition: {}, Offset: {}", 
                        topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                return result;
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi du message vers le topic: {}", topic, e);
                throw new RuntimeException("Erreur d'envoi Kafka", e);
            }
        });
    }

    /**
     * Envoi synchrone avec retry
     */
    @Retry(name = "kafkaSyncRetry", fallbackMethod = "kafkaSyncRetryFallback")
    @CircuitBreaker(name = "kafkaSyncCircuitBreaker", fallbackMethod = "kafkaSyncCircuitBreakerFallback")
    public SendResult<String, String> sendMessageSync(String topic, String message) {
        log.info("Tentative d'envoi synchrone vers le topic: {}", topic);
        
        try {
            SendResult<String, String> result = kafkaTemplate.send(topic, message).get();
            log.info("Message synchrone envoyé avec succès vers le topic: {}", topic);
            return result;
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi synchrone vers le topic: {}", topic, e);
            throw new RuntimeException("Erreur d'envoi Kafka synchrone", e);
        }
    }

    // Fallback methods pour Retry
    public CompletableFuture<SendResult<String, String>> kafkaRetryFallback(String topic, String message, Exception e) {
        log.warn("Fallback retry pour le topic: {} - Tentative échouée après tous les retry", topic);
        return CompletableFuture.failedFuture(new RuntimeException("Échec après retry pour le topic: " + topic, e));
    }

    public SendResult<String, String> kafkaSyncRetryFallback(String topic, String message, Exception e) {
        log.warn("Fallback retry synchrone pour le topic: {} - Tentative échouée après tous les retry", topic);
        throw new RuntimeException("Échec après retry synchrone pour le topic: " + topic, e);
    }

    // Fallback methods pour Circuit Breaker
    public CompletableFuture<SendResult<String, String>> kafkaCircuitBreakerFallback(String topic, String message, Exception e) {
        log.error("Circuit breaker ouvert pour le topic: {} - Service temporairement indisponible", topic);
        return CompletableFuture.failedFuture(new RuntimeException("Circuit breaker ouvert pour le topic: " + topic, e));
    }

    public SendResult<String, String> kafkaSyncCircuitBreakerFallback(String topic, String message, Exception e) {
        log.error("Circuit breaker synchrone ouvert pour le topic: {} - Service temporairement indisponible", topic);
        throw new RuntimeException("Circuit breaker synchrone ouvert pour le topic: " + topic, e);
    }

    // Fallback methods pour Bulkhead
    public CompletableFuture<SendResult<String, String>> kafkaBulkheadFallback(String topic, String message, Exception e) {
        log.warn("Bulkhead plein pour le topic: {} - Trop de requêtes simultanées", topic);
        return CompletableFuture.failedFuture(new RuntimeException("Bulkhead plein pour le topic: " + topic, e));
    }

    // Fallback methods pour Time Limiter
    public CompletableFuture<SendResult<String, String>> kafkaTimeLimiterFallback(String topic, String message, TimeoutException e) {
        log.warn("Timeout pour le topic: {} - L'opération a pris trop de temps", topic);
        return CompletableFuture.failedFuture(new RuntimeException("Timeout pour le topic: " + topic, e));
    }
} 