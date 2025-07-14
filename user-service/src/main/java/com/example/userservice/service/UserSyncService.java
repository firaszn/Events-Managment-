package com.example.userservice.service;

import com.example.userservice.entity.UserEntity;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserSyncService {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final PasswordEncoder passwordEncoder;
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 2000; // 2 secondes

    static class UserSyncException extends RuntimeException {
        public UserSyncException(String message) { super(message); }
        public UserSyncException(String message, Throwable cause) { super(message, cause); }
    }

    /**
     * Synchronise un utilisateur depuis Keycloak vers PostgreSQL
     */
    @Transactional
    public UserEntity syncUserFromKeycloak(String email) {
        UserRepresentation keycloakUser;

        try {
            keycloakUser = tryFetchKeycloakUserWithRetry(email);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interruption lors de la synchronisation", e);
            throw new UserSyncException("Interruption lors de la synchronisation", e);
        } catch (Exception e) {
            log.error("Impossible de récupérer l'utilisateur depuis Keycloak après {} tentatives", MAX_RETRIES, e);
            throw new UserSyncException("Impossible de récupérer l'utilisateur depuis Keycloak après " + MAX_RETRIES + " tentatives", e);
        }

        if (keycloakUser == null) {
            log.warn("Utilisateur {} non trouvé dans Keycloak après {} tentatives", email, MAX_RETRIES);
            throw new UserSyncException("Échec de la récupération de l'utilisateur Keycloak: utilisateur non trouvé");
        }

        try {
            return upsertUserFromKeycloak(keycloakUser, email);
        } catch (Exception e) {
            log.error("Erreur lors de la synchronisation de l'utilisateur {} depuis Keycloak: {}", email, e.getMessage(), e);
            throw new UserSyncException("Erreur lors de la synchronisation: " + e.getMessage(), e);
        }
    }

    private UserRepresentation tryFetchKeycloakUserWithRetry(String email) throws InterruptedException {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 1) {
                    // Délai exponentiel : 2s, 4s, 8s, 16s, 32s
                    long delay = (long) (INITIAL_RETRY_DELAY_MS * Math.pow(2.0, (attempt - 2)));                    log.info("Attente de {} secondes avant la tentative {} de récupération de l'utilisateur Keycloak", delay/1000, attempt);
                    Thread.sleep(delay);
                }
                log.info("Tentative {} de récupération de l'utilisateur Keycloak: {}", attempt, email);
                UserRepresentation keycloakUser = keycloakService.getUserByEmail(email);
                if (keycloakUser != null) {
                    log.info("Utilisateur trouvé dans Keycloak après {} tentative(s) - ID: {}, Email: {}", 
                            attempt, keycloakUser.getId(), keycloakUser.getEmail());
                    return keycloakUser;
                }
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Échec de la tentative {} de récupération de l'utilisateur Keycloak: {}", attempt, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    log.error("Échec de toutes les tentatives de récupération de l'utilisateur Keycloak", e);
                }
            }
        }
        return null;
    }

    private UserEntity upsertUserFromKeycloak(UserRepresentation keycloakUser, String email) {
        Optional<UserEntity> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            log.info("Utilisateur {} existe déjà en PostgreSQL - Mise à jour des informations", email);
            UserEntity user = existingUser.get();
            updateUserFromKeycloak(user, keycloakUser, email);
            UserEntity updatedUser = userRepository.save(user);
            log.info("Utilisateur {} mis à jour avec succès dans PostgreSQL", email);
            return updatedUser;
        } else {
            log.info("Création d'un nouvel utilisateur dans PostgreSQL pour {}", email);
            return createUserFromKeycloak(keycloakUser, email);
        }
    }

    private void updateUserFromKeycloak(UserEntity user, UserRepresentation keycloakUser, String email) {
        user.setFirstName(keycloakUser.getFirstName());
        user.setLastName(keycloakUser.getLastName());
        user.setUsername(keycloakUser.getUsername());
        user.setEnabled(keycloakUser.isEnabled());
        user.setUpdatedAt(LocalDateTime.now());
        List<String> userRoles = keycloakService.getUserRoles(keycloakUser.getId());
        if (!userRoles.isEmpty()) {
            String primaryRole = userRoles.get(0);
            try {
                user.setRole(UserEntity.Role.valueOf(primaryRole.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Rôle invalide {} pour l'utilisateur {}, utilisation du rôle USER par défaut", primaryRole, email);
                user.setRole(UserEntity.Role.USER);
            }
        }
    }

    private UserEntity createUserFromKeycloak(UserRepresentation keycloakUser, String email) {
        List<String> userRoles = keycloakService.getUserRoles(keycloakUser.getId());
        String primaryRole = userRoles.isEmpty() ? "USER" : userRoles.get(0);
        try {
            UserEntity.Role role = UserEntity.Role.valueOf(primaryRole.toUpperCase());
            log.info("Rôle assigné: {}", role);
        } catch (IllegalArgumentException e) {
            log.warn("Rôle invalide {}, utilisation du rôle USER par défaut", primaryRole);
            primaryRole = "USER";
        }
        UserEntity newUser = UserEntity.builder()
                .firstName(keycloakUser.getFirstName())
                .lastName(keycloakUser.getLastName())
                .email(keycloakUser.getEmail())
                .username(keycloakUser.getUsername() != null ? keycloakUser.getUsername() : keycloakUser.getEmail())
                .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Mot de passe aléatoire, inutilisé car géré par Keycloak
                .role(UserEntity.Role.valueOf(primaryRole.toUpperCase()))
                .enabled(keycloakUser.isEnabled())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        UserEntity savedUser = userRepository.save(newUser);
        log.info("Nouvel utilisateur {} créé avec succès dans PostgreSQL", email);
        return savedUser;
    }

    /**
     * Crée un utilisateur dans Keycloak ET PostgreSQL
     */
    @Transactional
    public UserEntity createUserInBothSystems(String email, String firstName, String lastName, 
                                            String password, String role, String phoneNumber) {
        try {
            // 1. Vérifier que l'utilisateur n'existe dans aucun système
            if (userRepository.findByEmail(email).isPresent()) {
                throw new IllegalStateException("Utilisateur existe déjà en PostgreSQL: " + email);
            }

            UserRepresentation keycloakUser = keycloakService.getUserByEmail(email);
            if (keycloakUser != null) {
                throw new IllegalStateException("Utilisateur existe déjà dans Keycloak: " + email);
            }

            // 2. Créer dans Keycloak d'abord
            String keycloakUserId = keycloakService.createUser(email, firstName, lastName, password, role);
            log.info("Utilisateur {} créé dans Keycloak avec ID: {}", email, keycloakUserId);

            // 3. Créer dans PostgreSQL
            UserEntity newUser = UserEntity.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .username(email) // Utiliser l'email comme username par défaut
                    .password(passwordEncoder.encode(password))
                    .phoneNumber(phoneNumber)
                    .role(UserEntity.Role.valueOf(role.toUpperCase()))
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            UserEntity savedUser = userRepository.save(newUser);
            log.info("Utilisateur {} créé dans PostgreSQL avec ID: {}", email, savedUser.getId());

            return savedUser;

        } catch (Exception e) {
            log.error("Erreur lors de la création de l'utilisateur {} dans les deux systèmes: {}", email, e.getMessage());
            // En cas d'erreur, essayer de nettoyer Keycloak si PostgreSQL a échoué
            try {
                UserRepresentation keycloakUser = keycloakService.getUserByEmail(email);
                if (keycloakUser != null) {
                    log.warn("Nettoyage nécessaire dans Keycloak pour l'utilisateur: {}", email);
                }
            } catch (Exception cleanupException) {
                log.error("Erreur lors du nettoyage: {}", cleanupException.getMessage());
            }
            throw new RuntimeException("Erreur lors de la création de l'utilisateur: " + e.getMessage());
        }
    }

    /**
     * Vérifie et synchronise un utilisateur lors du login
     */
    public UserEntity ensureUserSyncOnLogin(String email) {
        try {
            // Vérifier si l'utilisateur existe en PostgreSQL
            Optional<UserEntity> postgresUser = userRepository.findByEmail(email);
            
            if (postgresUser.isPresent()) {
                // L'utilisateur existe en PostgreSQL, pas besoin de synchronisation
                return postgresUser.get();
            }

            // L'utilisateur n'existe pas en PostgreSQL, essayer de le synchroniser depuis Keycloak
            log.info("Utilisateur {} non trouvé en PostgreSQL, tentative de synchronisation depuis Keycloak", email);
            return syncUserFromKeycloak(email);

        } catch (Exception e) {
            log.error("Erreur lors de la vérification de synchronisation pour {}: {}", email, e.getMessage());
            return null;
        }
    }
}
