package com.example.notificationservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailRetryService {

    private final JavaMailSender emailSender;

    /**
     * Envoi d'email avec retry et circuit breaker
     */
    @Retry(name = "emailRetry", fallbackMethod = "emailRetryFallback")
    @CircuitBreaker(name = "emailCircuitBreaker", fallbackMethod = "emailCircuitBreakerFallback")
    @Bulkhead(name = "emailBulkhead", fallbackMethod = "emailBulkheadFallback")
    @TimeLimiter(name = "emailTimeLimiter", fallbackMethod = "emailTimeLimiterFallback")
    public CompletableFuture<Void> sendEmailWithRetry(String to, String subject, String text) {
        log.info("Tentative d'envoi d'email à : {} | Sujet : {}", to, subject);
        
        return CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(text);
                
                emailSender.send(message);
                
                log.info("Email envoyé avec succès à : {}", to);
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de l'email à : {} | Sujet : {}", to, subject, e);
                throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
            }
        });
    }

    /**
     * Envoi synchrone d'email avec retry
     */
    @Retry(name = "emailSyncRetry", fallbackMethod = "emailSyncRetryFallback")
    @CircuitBreaker(name = "emailSyncCircuitBreaker", fallbackMethod = "emailSyncCircuitBreakerFallback")
    public void sendEmailSync(String to, String subject, String text) {
        log.info("Tentative d'envoi synchrone d'email à : {} | Sujet : {}", to, subject);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            emailSender.send(message);
            
            log.info("Email synchrone envoyé avec succès à : {}", to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi synchrone de l'email à : {} | Sujet : {}", to, subject, e);
            throw new RuntimeException("Erreur lors de l'envoi synchrone de l'email", e);
        }
    }

    /**
     * Envoi en batch avec retry
     */
    @Retry(name = "emailBatchRetry", fallbackMethod = "emailBatchRetryFallback")
    @CircuitBreaker(name = "emailBatchCircuitBreaker", fallbackMethod = "emailBatchCircuitBreakerFallback")
    @Bulkhead(name = "emailBatchBulkhead", fallbackMethod = "emailBatchBulkheadFallback")
    public CompletableFuture<Void> sendBatchEmailsWithRetry(String[] to, String subject, String text) {
        log.info("Tentative d'envoi en batch de {} emails | Sujet : {}", to.length, subject);
        
        return CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage[] messages = new SimpleMailMessage[to.length];
                for (int i = 0; i < to.length; i++) {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(to[i]);
                    message.setSubject(subject);
                    message.setText(text);
                    messages[i] = message;
                }
                
                emailSender.send(messages);
                
                log.info("Batch de {} emails envoyé avec succès", to.length);
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi en batch de {} emails | Sujet : {}", to.length, subject, e);
                throw new RuntimeException("Erreur lors de l'envoi en batch d'emails", e);
            }
        });
    }

    // Fallback methods pour Retry
    public CompletableFuture<Void> emailRetryFallback(String to, String subject, String text, Exception e) {
        log.warn("Fallback retry pour l'email à : {} - Tentative échouée après tous les retry", to);
        return CompletableFuture.failedFuture(new RuntimeException("Échec après retry pour l'email à: " + to, e));
    }

    public void emailSyncRetryFallback(String to, String subject, String text, Exception e) {
        log.warn("Fallback retry synchrone pour l'email à : {} - Tentative échouée après tous les retry", to);
        throw new RuntimeException("Échec après retry synchrone pour l'email à: " + to, e);
    }

    public CompletableFuture<Void> emailBatchRetryFallback(String[] to, String subject, String text, Exception e) {
        log.warn("Fallback retry batch pour {} emails - Tentative échouée après tous les retry", to.length);
        return CompletableFuture.failedFuture(new RuntimeException("Échec après retry batch pour " + to.length + " emails", e));
    }

    // Fallback methods pour Circuit Breaker
    public CompletableFuture<Void> emailCircuitBreakerFallback(String to, String subject, String text, Exception e) {
        log.error("Circuit breaker ouvert pour l'email à : {} - Service email temporairement indisponible", to);
        return CompletableFuture.failedFuture(new RuntimeException("Circuit breaker ouvert pour l'email à: " + to, e));
    }

    public void emailSyncCircuitBreakerFallback(String to, String subject, String text, Exception e) {
        log.error("Circuit breaker synchrone ouvert pour l'email à : {} - Service email temporairement indisponible", to);
        throw new RuntimeException("Circuit breaker synchrone ouvert pour l'email à: " + to, e);
    }

    public CompletableFuture<Void> emailBatchCircuitBreakerFallback(String[] to, String subject, String text, Exception e) {
        log.error("Circuit breaker batch ouvert pour {} emails - Service email temporairement indisponible", to.length);
        return CompletableFuture.failedFuture(new RuntimeException("Circuit breaker batch ouvert pour " + to.length + " emails", e));
    }

    // Fallback methods pour Bulkhead
    public CompletableFuture<Void> emailBulkheadFallback(String to, String subject, String text, Exception e) {
        log.warn("Bulkhead plein pour l'email à : {} - Trop de requêtes simultanées", to);
        return CompletableFuture.failedFuture(new RuntimeException("Bulkhead plein pour l'email à: " + to, e));
    }

    public CompletableFuture<Void> emailBatchBulkheadFallback(String[] to, String subject, String text, Exception e) {
        log.warn("Bulkhead batch plein pour {} emails - Trop de requêtes simultanées", to.length);
        return CompletableFuture.failedFuture(new RuntimeException("Bulkhead batch plein pour " + to.length + " emails", e));
    }

    // Fallback methods pour Time Limiter
    public CompletableFuture<Void> emailTimeLimiterFallback(String to, String subject, String text, TimeoutException e) {
        log.warn("Timeout pour l'email à : {} - L'opération a pris trop de temps", to);
        return CompletableFuture.failedFuture(new RuntimeException("Timeout pour l'email à: " + to, e));
    }
} 