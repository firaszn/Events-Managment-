package com.example.userservice.controller;

import com.example.userservice.entity.UserEntity;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.model.*;
import com.example.userservice.service.UserService;
import com.example.userservice.service.KeycloakUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST pour la gestion des utilisateurs
 * Utilise exclusivement des DTOs pour les échanges avec les clients
 *
 * Endpoints :
 * - GET /api/users - Liste tous les utilisateurs (ADMIN)
 * - GET /api/users/{id} - Détails d'un utilisateur (ADMIN)
 * - POST /api/users - Créer un utilisateur (ADMIN)
 * - PUT /api/users/{id} - Modifier un utilisateur (ADMIN)
 * - DELETE /api/users/{id} - Supprimer un utilisateur (ADMIN)
 * - GET /api/users/profile - Profil de l'utilisateur connecté
 * - PUT /api/users/profile - Modifier son profil
 * - PUT /api/users/change-password - Changer son mot de passe
 * - GET /api/users/download-pdf - Télécharger la liste des utilisateurs en PDF (ADMIN)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final KeycloakUserService keycloakUserService;

    /**
     * GET /api/users - Récupérer tous les utilisateurs (ADMIN uniquement)
     * Accepte les tokens JWT Keycloak
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(Authentication authentication) {
        log.info("Requête de récupération de tous les utilisateurs avec token Keycloak");
        try {
            // Vérifier le rôle ADMIN depuis le token Keycloak
            if (!keycloakUserService.isCurrentUserAdmin(authentication)) {
                log.warn("Accès refusé - rôle ADMIN requis");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Récupérer l'email de l'admin connecté
            String currentUserEmail = keycloakUserService.getEmailFromToken(authentication);
            
            List<UserEntity> users = keycloakUserService.getAllUsers();
            // Filtrer l'administrateur connecté de la liste
            users = users.stream()
                .filter(user -> !user.getEmail().equals(currentUserEmail))
                .toList();
            List<UserResponse> userResponses = userMapper.toUserResponseList(users);
            log.info("Récupération de {} utilisateurs (sans admin)", users.size());
            return ResponseEntity.ok(userResponses);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des utilisateurs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/users/{id} - Récupérer un utilisateur par ID (ADMIN uniquement)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("Requête de récupération de l'utilisateur avec l'ID : {}", id);
        try {
            UserEntity user = userService.findById(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            UserResponse userResponse = userMapper.toUserResponse(user);
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'utilisateur avec l'ID : {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/users - Créer un nouvel utilisateur (ADMIN uniquement)
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest) {
        log.info("Requête de création d'utilisateur : {}", userRequest.getUsername());
        try {
            UserEntity userEntity = userMapper.toUserEntity(userRequest);
            UserEntity createdUser = userService.createUser(userEntity);
            UserResponse userResponse = userMapper.toUserResponse(createdUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
        } catch (IllegalStateException e) {
            log.warn("Erreur de validation lors de la création de l'utilisateur : {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur lors de la création de l'utilisateur", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * PUT /api/users/{id} - Modifier un utilisateur (ADMIN uniquement)
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest updateRequest) {
        log.info("Requête de mise à jour de l'utilisateur avec l'ID : {}", id);
        try {
            UserEntity existingUser = userService.findById(id);
            if (existingUser == null) {
                return ResponseEntity.notFound().build();
            }

            userMapper.updateUserEntity(existingUser, updateRequest);
            UserEntity updatedUser = userService.updateUser(id, existingUser);
            UserResponse userResponse = userMapper.toUserResponse(updatedUser);
            return ResponseEntity.ok(userResponse);
        } catch (IllegalStateException e) {
            log.warn("Erreur de validation lors de la mise à jour de l'utilisateur : {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de l'utilisateur avec l'ID : {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/users/profile - Récupérer le profil de l'utilisateur connecté
     * Accepte les tokens JWT Keycloak
     */
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        log.info("Requête de récupération du profil utilisateur avec token Keycloak");
        try {
            UserEntity user = keycloakUserService.getCurrentUserFromToken(authentication);
            UserResponse userResponse = userMapper.toUserResponse(user);
            log.info("Profil récupéré pour l'utilisateur: {}", user.getEmail());
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du profil utilisateur", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * PUT /api/users/profile - Modifier le profil de l'utilisateur connecté
     * Accepte les tokens JWT Keycloak et synchronise avec Keycloak
     */
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UserUpdateRequest updateRequest,
                                                     Authentication authentication) {
        log.info("Requête de mise à jour du profil reçue : {}", updateRequest);
        log.info("Requête de mise à jour du profil utilisateur avec token Keycloak");
        try {
            UserEntity currentUser = keycloakUserService.getCurrentUserFromToken(authentication);

            // Validation supplémentaire pour le password
            if (updateRequest.getPassword() != null && updateRequest.getPassword().trim().isEmpty()) {
                log.warn("Tentative de mise à jour avec un mot de passe vide");
                return ResponseEntity.badRequest().build();
            }

            // Mettre à jour dans Keycloak ET PostgreSQL
            UserEntity updatedUser = keycloakUserService.updateUserProfile(
                    currentUser,
                    updateRequest.getFirstName(),
                    updateRequest.getLastName(),
                    updateRequest.getEmail(),
                    updateRequest.getPhoneNumber(),
                    updateRequest.getPassword() // Ajouter le mot de passe
            );

            UserResponse userResponse = userMapper.toUserResponse(updatedUser);
            log.info("Profil mis à jour avec succès pour l'utilisateur: {}", updatedUser.getEmail());
            return ResponseEntity.ok(userResponse);
        } catch (IllegalStateException e) {
            log.warn("Erreur de validation lors de la mise à jour du profil : {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du profil utilisateur", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * PUT /api/users/change-password - Changer le mot de passe de l'utilisateur connecté
     */
    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequest passwordRequest) {
        log.info("Requête de changement de mot de passe");
        try {
            // Vérifier que les mots de passe correspondent
            if (!passwordRequest.getNewPassword().equals(passwordRequest.getConfirmPassword())) {
                log.warn("Les mots de passe de confirmation ne correspondent pas");
                return ResponseEntity.badRequest().build();
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserEntity currentUser = userService.findByEmail(auth.getName());

            if (currentUser == null) {
                return ResponseEntity.notFound().build();
            }

            // Changer le mot de passe (la validation de l'ancien mot de passe se fait dans le service)
            userService.changePassword(currentUser.getId(), passwordRequest.getOldPassword(), passwordRequest.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.warn("Erreur lors du changement de mot de passe : {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur lors du changement de mot de passe", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * DELETE /api/users/{id} - Supprimer un utilisateur (ADMIN uniquement)
     * Accepte les tokens JWT Keycloak et supprime de Keycloak ET PostgreSQL
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        log.info("Requête de suppression de l'utilisateur avec l'ID : {} avec token Keycloak", id);
        try {
            // Vérifier le rôle ADMIN depuis le token Keycloak
            if (!keycloakUserService.isCurrentUserAdmin(authentication)) {
                log.warn("Accès refusé - rôle ADMIN requis pour supprimer l'utilisateur {}", id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Supprimer de Keycloak ET PostgreSQL
            keycloakUserService.deleteUser(id);
            log.info("Utilisateur {} supprimé avec succès des deux systèmes", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Erreur lors de la suppression de l'utilisateur avec l'ID : {} - {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'utilisateur avec l'ID : {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/users/download-pdf - Télécharger la liste des utilisateurs en PDF (ADMIN uniquement)
     */

}