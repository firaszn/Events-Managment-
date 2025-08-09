package com.example.notificationservice.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final BulkheadRegistry bulkheadRegistry;

    /**
     * Obtenir les métriques de tous les circuit breakers
     */
    public Map<String, Object> getCircuitBreakerMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            CircuitBreaker.State state = circuitBreaker.getState();
            CircuitBreaker.Metrics metrics1 = circuitBreaker.getMetrics();
            
            Map<String, Object> circuitBreakerMetrics = new HashMap<>();
            circuitBreakerMetrics.put("state", state.name());
            circuitBreakerMetrics.put("failureRate", metrics1.getFailureRate());
            circuitBreakerMetrics.put("numberOfFailedCalls", metrics1.getNumberOfFailedCalls());
            circuitBreakerMetrics.put("numberOfSuccessfulCalls", metrics1.getNumberOfSuccessfulCalls());
            circuitBreakerMetrics.put("numberOfNotPermittedCalls", metrics1.getNumberOfNotPermittedCalls());
            
            metrics.put(circuitBreaker.getName(), circuitBreakerMetrics);
        });
        
        return metrics;
    }

    /**
     * Obtenir les métriques de tous les retry
     */
    public Map<String, Object> getRetryMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        retryRegistry.getAllRetries().forEach(retry -> {
            Retry.Metrics retryMetrics = retry.getMetrics();
            
            Map<String, Object> retryMetricsMap = new HashMap<>();
            retryMetricsMap.put("numberOfSuccessfulCallsWithoutRetryAttempt", retryMetrics.getNumberOfSuccessfulCallsWithoutRetryAttempt());
            retryMetricsMap.put("numberOfSuccessfulCallsWithRetryAttempt", retryMetrics.getNumberOfSuccessfulCallsWithRetryAttempt());
            retryMetricsMap.put("numberOfFailedCallsWithoutRetryAttempt", retryMetrics.getNumberOfFailedCallsWithoutRetryAttempt());
            retryMetricsMap.put("numberOfFailedCallsWithRetryAttempt", retryMetrics.getNumberOfFailedCallsWithRetryAttempt());
            
            metrics.put(retry.getName(), retryMetricsMap);
        });
        
        return metrics;
    }

    /**
     * Obtenir les métriques de tous les bulkheads
     */
    public Map<String, Object> getBulkheadMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        bulkheadRegistry.getAllBulkheads().forEach(bulkhead -> {
            Bulkhead.Metrics bulkheadMetrics = bulkhead.getMetrics();
            
            Map<String, Object> bulkheadMetricsMap = new HashMap<>();
            bulkheadMetricsMap.put("availableConcurrentCalls", bulkheadMetrics.getAvailableConcurrentCalls());
            bulkheadMetricsMap.put("maxAllowedConcurrentCalls", bulkheadMetrics.getMaxAllowedConcurrentCalls());
            
            metrics.put(bulkhead.getName(), bulkheadMetricsMap);
        });
        
        return metrics;
    }

    /**
     * Obtenir toutes les métriques Resilience4j
     */
    public Map<String, Object> getAllResilience4jMetrics() {
        Map<String, Object> allMetrics = new HashMap<>();
        allMetrics.put("circuitBreakers", getCircuitBreakerMetrics());
        allMetrics.put("retries", getRetryMetrics());
        allMetrics.put("bulkheads", getBulkheadMetrics());
        
        return allMetrics;
    }

    /**
     * Vérifier la santé des services
     */
    public Map<String, String> getHealthStatus() {
        Map<String, String> healthStatus = new HashMap<>();
        
        // Vérifier les circuit breakers
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            CircuitBreaker.State state = circuitBreaker.getState();
            healthStatus.put("circuitBreaker." + circuitBreaker.getName(), 
                state == CircuitBreaker.State.CLOSED ? "HEALTHY" : "UNHEALTHY");
        });
        
        // Vérifier les bulkheads
        bulkheadRegistry.getAllBulkheads().forEach(bulkhead -> {
            Bulkhead.Metrics metrics = bulkhead.getMetrics();
            healthStatus.put("bulkhead." + bulkhead.getName(), 
                metrics.getAvailableConcurrentCalls() > 0 ? "HEALTHY" : "UNHEALTHY");
        });
        
        return healthStatus;
    }

    /**
     * Log des métriques pour monitoring
     */
    public void logMetrics() {
        log.info("=== Métriques Resilience4j ===");
        
        // Log des circuit breakers
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            CircuitBreaker.State state = circuitBreaker.getState();
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            log.info("Circuit Breaker '{}': State={}, FailureRate={}%, FailedCalls={}, SuccessfulCalls={}", 
                circuitBreaker.getName(), 
                state.name(), 
                String.format("%.2f", metrics.getFailureRate() * 100),
                metrics.getNumberOfFailedCalls(),
                metrics.getNumberOfSuccessfulCalls());
        });
        
        // Log des retry
        retryRegistry.getAllRetries().forEach(retry -> {
            Retry.Metrics metrics = retry.getMetrics();
            log.info("Retry '{}': SuccessfulWithoutRetry={}, SuccessfulWithRetry={}, FailedWithoutRetry={}, FailedWithRetry={}", 
                retry.getName(),
                metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt(),
                metrics.getNumberOfSuccessfulCallsWithRetryAttempt(),
                metrics.getNumberOfFailedCallsWithoutRetryAttempt(),
                metrics.getNumberOfFailedCallsWithRetryAttempt());
        });
        
        // Log des bulkheads
        bulkheadRegistry.getAllBulkheads().forEach(bulkhead -> {
            Bulkhead.Metrics metrics = bulkhead.getMetrics();
            log.info("Bulkhead '{}': AvailableCalls={}, MaxAllowedCalls={}", 
                bulkhead.getName(),
                metrics.getAvailableConcurrentCalls(),
                metrics.getMaxAllowedConcurrentCalls());
        });
        
        log.info("=== Fin des métriques ===");
    }
} 