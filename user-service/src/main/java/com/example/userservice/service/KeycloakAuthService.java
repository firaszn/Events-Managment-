package com.example.userservice.service;

import com.example.userservice.auth.KeycloakAuthResponse;
import com.example.userservice.auth.KeycloakLoginRequest;
import com.example.userservice.auth.KeycloakRegisterRequest;
import com.example.userservice.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakAuthService {

    private final KeycloakService keycloakService;
    private final UserSyncService userSyncService;

    @Value("${keycloak.admin.server-url:http://localhost:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.admin.realm:RepasKeycloak}")
    private String realm;

    public KeycloakAuthResponse registerUser(KeycloakRegisterRequest request) {
        try {
            // Créer l'utilisateur dans Keycloak ET PostgreSQL via le service de synchronisation
            var createdUser = userSyncService.createUserInBothSystems(
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPassword(),
                    request.getRole() != null ? request.getRole() : "USER",
                    request.getPhoneNumber()
            );

            // Récupérer l'utilisateur Keycloak pour obtenir son ID
            UserRepresentation keycloakUser = keycloakService.getUserByEmail(request.getEmail());

            // Construire l'URL de connexion Keycloak
            String keycloakLoginUrl = buildKeycloakLoginUrl();

            return KeycloakAuthResponse.builder()
                    .message("Utilisateur créé avec succès dans Keycloak et PostgreSQL")
                    .email(request.getEmail())
                    .keycloakUserId(keycloakUser != null ? keycloakUser.getId() : null)
                    .role(createdUser.getRole().name())
                    .keycloakLoginUrl(keycloakLoginUrl)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'utilisateur: {}", e.getMessage());
            return KeycloakAuthResponse.builder()
                    .message("Erreur lors de la création de l'utilisateur: " + e.getMessage())
                    .email(request.getEmail())
                    .build();
        }
    }

    public KeycloakAuthResponse loginUser(KeycloakLoginRequest request) {
        try {
            // Vérifier si l'utilisateur existe dans Keycloak
            UserRepresentation user = keycloakService.getUserByEmail(request.getEmail());
            if (user == null) {
                return KeycloakAuthResponse.builder()
                        .message("Utilisateur non trouvé dans Keycloak")
                        .email(request.getEmail())
                        .build();
            }

            // Récupérer le token d'accès Keycloak
            String accessToken = keycloakService.getKeycloakAccessToken(request.getEmail(), request.getPassword());

            if (accessToken != null) {
                // Synchroniser l'utilisateur avec PostgreSQL si nécessaire
                UserEntity postgresUser = userSyncService.ensureUserSyncOnLogin(request.getEmail());
                if (postgresUser != null) {
                    log.info("Utilisateur {} synchronisé avec PostgreSQL", request.getEmail());
                }

                // Récupérer les rôles de l'utilisateur
                var userRoles = keycloakService.getUserRoles(user.getId());
                String primaryRole;

                if (userRoles.isEmpty()) {
                    // Si aucun rôle assigné, assigner USER par défaut
                    log.info("Aucun rôle trouvé pour {}, assignation du rôle USER par défaut", request.getEmail());
                    keycloakService.assignRoleToUser(user.getId(), "USER");
                    primaryRole = "USER";
                } else {
                    primaryRole = userRoles.get(0);
                }

                // Construire l'URL de connexion Keycloak
                String keycloakLoginUrl = buildKeycloakLoginUrl();

                return KeycloakAuthResponse.builder()
                        .message("Authentification réussie avec token JWT Keycloak")
                        .email(request.getEmail())
                        .keycloakUserId(user.getId())
                        .role(primaryRole)
                        .keycloakLoginUrl(keycloakLoginUrl)
                        .accessToken(accessToken)
                        .tokenType("Bearer")
                        .expiresIn(3600L) // 1 heure par défaut
                        .build();
            } else {
                return KeycloakAuthResponse.builder()
                        .message("Email ou mot de passe incorrect")
                        .email(request.getEmail())
                        .build();
            }

        } catch (Exception e) {
            log.error("Erreur lors de la connexion de l'utilisateur: {}", e.getMessage());
            return KeycloakAuthResponse.builder()
                    .message("Erreur lors de la connexion: " + e.getMessage())
                    .email(request.getEmail())
                    .build();
        }
    }

    private String buildKeycloakLoginUrl() {
        return String.format("%s/realms/%s/account", keycloakServerUrl, realm);
    }

    public String getKeycloakAdminUrl() {
        return String.format("%s/admin/master/console/#/%s/users", keycloakServerUrl, realm);
    }

    public com.example.userservice.entity.UserEntity syncUserFromKeycloak(String email) {
        return userSyncService.syncUserFromKeycloak(email);
    }
}
