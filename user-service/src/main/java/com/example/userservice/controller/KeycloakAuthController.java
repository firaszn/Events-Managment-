package com.example.userservice.controller;

import com.example.userservice.auth.KeycloakAuthResponse;
import com.example.userservice.auth.KeycloakLoginRequest;
import com.example.userservice.auth.KeycloakRegisterRequest;
import com.example.userservice.service.KeycloakAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/keycloak")
@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthController {

    private final KeycloakAuthService keycloakAuthService;

    @PostMapping("/register")
    public ResponseEntity<KeycloakAuthResponse> registerWithKeycloak(@RequestBody KeycloakRegisterRequest request) {
        log.info("Tentative d'enregistrement Keycloak pour l'email: {}", request.getEmail());
        
        KeycloakAuthResponse response = keycloakAuthService.registerUser(request);
        
        if (response.getKeycloakUserId() != null) {
            log.info("Utilisateur créé avec succès dans Keycloak: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } else {
            log.warn("Échec de création de l'utilisateur dans Keycloak: {}", request.getEmail());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<KeycloakAuthResponse> loginWithKeycloak(@RequestBody KeycloakLoginRequest request) {
        log.info("Tentative de connexion Keycloak pour l'email: {}", request.getEmail());
        
        KeycloakAuthResponse response = keycloakAuthService.loginUser(request);
        
        if (response.getKeycloakUserId() != null) {
            log.info("Connexion Keycloak réussie pour: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } else {
            log.warn("Échec de connexion Keycloak pour: {}", request.getEmail());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/admin-url")
    public ResponseEntity<String> getKeycloakAdminUrl() {
        String adminUrl = keycloakAuthService.getKeycloakAdminUrl();
        return ResponseEntity.ok(adminUrl);
    }

    @PostMapping("/sync-from-keycloak")
    public ResponseEntity<String> syncUserFromKeycloak(@RequestParam String email) {
        log.info("Demande de synchronisation de l'utilisateur {} depuis Keycloak", email);

        try {
            var syncedUser = keycloakAuthService.syncUserFromKeycloak(email);
            if (syncedUser != null) {
                return ResponseEntity.ok("Utilisateur " + email + " synchronisé avec succès depuis Keycloak vers PostgreSQL");
            } else {
                return ResponseEntity.badRequest().body("Impossible de synchroniser l'utilisateur " + email);
            }
        } catch (Exception e) {
            log.error("Erreur lors de la synchronisation: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    @GetMapping("/info")
    public ResponseEntity<String> getKeycloakInfo() {
        return ResponseEntity.ok("""
            Endpoints Keycloak disponibles:
            - POST /auth/keycloak/register : Créer un utilisateur dans Keycloak ET PostgreSQL
            - POST /auth/keycloak/login : Valider les credentials avec Keycloak (sync auto)
            - POST /auth/keycloak/sync-from-keycloak?email=xxx : Synchroniser un utilisateur depuis Keycloak
            - GET /auth/keycloak/admin-url : Obtenir l'URL d'administration Keycloak
            - Dashboard Keycloak : http://localhost:8080
            - Realm : RepasKeycloak
            - Base PostgreSQL : Synchronisation automatique
            """);
    }
}
