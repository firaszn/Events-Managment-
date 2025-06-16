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

/**
 * Service de synchronisation entre Keycloak et PostgreSQL
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserSyncService {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Synchronise un utilisateur depuis Keycloak vers PostgreSQL
     */
    @Transactional
    public UserEntity syncUserFromKeycloak(String email) {
        try {
            // Récupérer l'utilisateur depuis Keycloak
            UserRepresentation keycloakUser = keycloakService.getUserByEmail(email);
            if (keycloakUser == null) {
                log.warn("Utilisateur {} non trouvé dans Keycloak", email);
                return null;
            }

            // Vérifier s'il existe déjà en PostgreSQL
            Optional<UserEntity> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent()) {
                log.info("Utilisateur {} existe déjà en PostgreSQL", email);
                return existingUser.get();
            }

            // Récupérer les rôles depuis Keycloak
            List<String> userRoles = keycloakService.getUserRoles(keycloakUser.getId());
            String primaryRole = userRoles.isEmpty() ? "USER" : userRoles.get(0);

            // Créer l'utilisateur en PostgreSQL
            UserEntity newUser = UserEntity.builder()
                    .firstName(keycloakUser.getFirstName())
                    .lastName(keycloakUser.getLastName())
                    .email(keycloakUser.getEmail())
                    .username(keycloakUser.getUsername())
                    .password(passwordEncoder.encode("KEYCLOAK_MANAGED")) // Mot de passe géré par Keycloak
                    .role(UserEntity.Role.valueOf(primaryRole))
                    .enabled(keycloakUser.isEnabled())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            UserEntity savedUser = userRepository.save(newUser);
            log.info("Utilisateur {} synchronisé depuis Keycloak vers PostgreSQL", email);
            
            return savedUser;

        } catch (Exception e) {
            log.error("Erreur lors de la synchronisation de l'utilisateur {} depuis Keycloak: {}", email, e.getMessage());
            return null;
        }
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
                    // TODO: Implémenter la suppression dans Keycloak si nécessaire
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
