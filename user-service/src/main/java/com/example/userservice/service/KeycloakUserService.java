package com.example.userservice.service;

import com.example.userservice.entity.UserEntity;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service pour la gestion des utilisateurs avec synchronisation Keycloak
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserService {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final UserSyncService userSyncService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Récupère l'utilisateur connecté depuis le token JWT Keycloak
     */
    public UserEntity getCurrentUserFromToken(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            log.info("Récupération de l'utilisateur connecté: {}", email);
            
            // Assurer la synchronisation
            UserEntity user = userSyncService.ensureUserSyncOnLogin(email);
            if (user == null) {
                throw new KeycloakUserServiceException("Utilisateur non trouvé: " + email);
            }
            return user;
        }
        throw new KeycloakUserServiceException("Token JWT invalide");
    }

    /**
     * Met à jour le profil utilisateur dans Keycloak ET PostgreSQL
     */
    @Transactional
    public UserEntity updateUserProfile(UserEntity currentUser, String firstName, String lastName, String email, String phoneNumber, String password) {
        try {
            updateUserFields(currentUser, firstName, lastName, phoneNumber, password);
            String oldEmail = currentUser.getEmail();
            validateAndSetEmail(currentUser, email);
            UserEntity updatedUser = userRepository.save(currentUser);
            log.info("Utilisateur mis à jour dans PostgreSQL: {}", updatedUser.getEmail());
            updateUserInKeycloak(oldEmail, firstName, lastName, email, password);
            return updatedUser;
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du profil utilisateur: {}", e.getMessage());
            throw new KeycloakUserServiceException("Erreur lors de la mise à jour: " + e.getMessage(), e);
        }
    }

    private void updateUserFields(UserEntity user, String firstName, String lastName, String phoneNumber, String password) {
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (phoneNumber != null) user.setPhoneNumber(phoneNumber);
        if (password != null && !password.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
            log.info("Mot de passe mis à jour pour l'utilisateur: {}", user.getEmail());
        }
    }

    private void validateAndSetEmail(UserEntity user, String newEmail) {
        String oldEmail = user.getEmail();
        if (newEmail != null && !newEmail.equals(oldEmail)) {
            if (userRepository.findByEmail(newEmail).isPresent()) {
                throw new IllegalStateException("Email déjà utilisé: " + newEmail);
            }
            user.setEmail(newEmail);
        }
    }

    /**
     * Met à jour un utilisateur dans Keycloak
     */
    private void updateUserInKeycloak(String currentEmail, String firstName, String lastName, String newEmail, String password) {
        try {
            UserRepresentation keycloakUser = keycloakService.getUserByEmail(currentEmail);
            if (keycloakUser != null) {
                boolean updated = false;

                if (firstName != null && !firstName.equals(keycloakUser.getFirstName())) {
                    keycloakUser.setFirstName(firstName);
                    updated = true;
                }

                if (lastName != null && !lastName.equals(keycloakUser.getLastName())) {
                    keycloakUser.setLastName(lastName);
                    updated = true;
                }

                if (newEmail != null && !newEmail.equals(keycloakUser.getEmail())) {
                    keycloakUser.setEmail(newEmail);
                    keycloakUser.setUsername(newEmail);
                    updated = true;
                }

                if (updated) {
                    keycloakService.updateUser(keycloakUser);
                    log.info("Utilisateur mis à jour dans Keycloak: {}", newEmail != null ? newEmail : currentEmail);
                }

                // Mettre à jour le mot de passe séparément si fourni
                if (password != null && !password.trim().isEmpty()) {
                    keycloakService.setUserPassword(keycloakUser.getId(), password);
                    log.info("Mot de passe mis à jour dans Keycloak pour: {}", keycloakUser.getEmail());
                }
            }
        } catch (Exception e) {
            log.warn("Impossible de mettre à jour l'utilisateur dans Keycloak: {}", e.getMessage());
            // Ne pas faire échouer la transaction PostgreSQL si Keycloak échoue
        }
    }

    /**
     * Supprime un utilisateur dans Keycloak ET PostgreSQL
     */
    @Transactional
    public void deleteUser(Long userId) {
        try {
            // 1. Récupérer l'utilisateur
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new KeycloakUserServiceException("Utilisateur non trouvé: " + userId));
            
            String email = user.getEmail();
            
            // 2. Supprimer de PostgreSQL
            userRepository.deleteById(userId);
            log.info("Utilisateur {} supprimé de PostgreSQL", email);
            
            // 3. Supprimer de Keycloak
            deleteUserFromKeycloak(email);
            
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'utilisateur {}: {}", userId, e.getMessage());
            throw new KeycloakUserServiceException("Erreur lors de la suppression: " + e.getMessage(), e);
        }
    }

    /**
     * Supprime un utilisateur de Keycloak
     */
    private void deleteUserFromKeycloak(String email) {
        try {
            UserRepresentation keycloakUser = keycloakService.getUserByEmail(email);
            if (keycloakUser != null) {
                keycloakService.deleteUser(keycloakUser.getId());
                log.info("Utilisateur {} supprimé de Keycloak", email);
            }
        } catch (Exception e) {
            log.warn("Impossible de supprimer l'utilisateur {} de Keycloak: {}", email, e.getMessage());
            // Ne pas faire échouer la transaction si Keycloak échoue
        }
    }

    /**
     * Récupère tous les utilisateurs avec synchronisation
     */
    public List<UserEntity> getAllUsers() {
        return userService.getAllUsers();
    }


    /**
     * Vérifie si l'utilisateur connecté a le rôle ADMIN
     */
    public boolean isCurrentUserAdmin(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            // Vérifier les rôles dans le token Keycloak
            var realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
                return roles.contains("ADMIN");
            }
        }
        return false;
    }

    /**
     * Récupère le rôle de l'utilisateur depuis le token JWT
     */
    public String getRoleFromToken(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            var realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
                // Chercher USER ou ADMIN dans les rôles
                for (Object role : roles) {
                    if ("ADMIN".equals(role.toString()) || "USER".equals(role.toString())) {
                        return role.toString();
                    }
                }
            }
        }
        return "USER"; // Rôle par défaut
    }

    /**
     * Extrait l'email du token JWT
     */
    public String getEmailFromToken(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("email");
        }
        throw new KeycloakUserServiceException("Token JWT invalide");
    }
}

class KeycloakUserServiceException extends RuntimeException {
    public KeycloakUserServiceException(String message) { super(message); }
    public KeycloakUserServiceException(String message, Throwable cause) { super(message, cause); }
}
