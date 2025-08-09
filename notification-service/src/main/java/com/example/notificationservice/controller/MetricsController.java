package com.example.notificationservice.controller;

import com.example.notificationservice.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Slf4j
public class MetricsController {

    private final MonitoringService monitoringService;

    /**
     * Obtenir toutes les métriques Resilience4j
     */
    @GetMapping("/resilience4j")
    public ResponseEntity<Map<String, Object>> getResilience4jMetrics() {
        log.info("Demande des métriques Resilience4j");
        Map<String, Object> metrics = monitoringService.getAllResilience4jMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Obtenir les métriques des circuit breakers
     */
    @GetMapping("/resilience4j/circuit-breakers")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerMetrics() {
        log.info("Demande des métriques des circuit breakers");
        Map<String, Object> metrics = monitoringService.getCircuitBreakerMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Obtenir les métriques des retry
     */
    @GetMapping("/resilience4j/retries")
    public ResponseEntity<Map<String, Object>> getRetryMetrics() {
        log.info("Demande des métriques des retry");
        Map<String, Object> metrics = monitoringService.getRetryMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Obtenir les métriques des bulkheads
     */
    @GetMapping("/resilience4j/bulkheads")
    public ResponseEntity<Map<String, Object>> getBulkheadMetrics() {
        log.info("Demande des métriques des bulkheads");
        Map<String, Object> metrics = monitoringService.getBulkheadMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Obtenir le statut de santé des services
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> getHealthStatus() {
        log.info("Demande du statut de santé");
        Map<String, String> healthStatus = monitoringService.getHealthStatus();
        return ResponseEntity.ok(healthStatus);
    }

    /**
     * Forcer le log des métriques
     */
    @PostMapping("/log")
    public ResponseEntity<String> logMetrics() {
        log.info("Demande de log des métriques");
        monitoringService.logMetrics();
        return ResponseEntity.ok("Métriques loggées avec succès");
    }

    /**
     * Reset des métriques (pour les tests)
     */
    @PostMapping("/reset")
    public ResponseEntity<String> resetMetrics() {
        log.info("Demande de reset des métriques");
        // Note: Cette fonctionnalité nécessiterait une implémentation spécifique
        // pour réinitialiser les métriques Resilience4j
        return ResponseEntity.ok("Reset des métriques demandé (non implémenté)");
    }
} 