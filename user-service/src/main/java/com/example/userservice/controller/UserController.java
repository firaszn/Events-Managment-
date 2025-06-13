package com.example.userservice.controller;

import com.example.userservice.entity.UserEntity;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.model.*;
import com.example.userservice.service.UserService;
import com.example.userservice.service.PDFService;
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
    private final PDFService pdfService;
    private final UserMapper userMapper;

    /**
     * GET /api/users - Récupérer tous les utilisateurs (ADMIN uniquement)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("Requête de récupération de tous les utilisateurs");
        try {
            List<UserEntity> users = userService.getAllUsers();
            List<UserResponse> userResponses = userMapper.toUserResponseList(users);
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
     */
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getCurrentUser() {
        log.info("Requête de récupération du profil utilisateur");
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserEntity user = userService.findByEmail(auth.getName());
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            UserResponse userResponse = userMapper.toUserResponse(user);
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du profil utilisateur", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * PUT /api/users/profile - Modifier le profil de l'utilisateur connecté
     */
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UserUpdateRequest updateRequest) {
        log.info("Requête de mise à jour du profil utilisateur");
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserEntity currentUser = userService.findByEmail(auth.getName());

            if (currentUser == null) {
                return ResponseEntity.notFound().build();
            }

            // Vérifier si l'utilisateur essaie de changer son email vers un email existant
            if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(currentUser.getEmail())) {
                if (userService.findByEmail(updateRequest.getEmail()) != null) {
                    log.warn("Tentative de changement d'email vers un email déjà existant : {}", updateRequest.getEmail());
                    return ResponseEntity.badRequest().build();
                }
            }

            // Validation supplémentaire pour le password
            if (updateRequest.getPassword() != null && updateRequest.getPassword().trim().isEmpty()) {
                log.warn("Tentative de mise à jour avec un mot de passe vide");
                return ResponseEntity.badRequest().build();
            }

            userMapper.updateUserEntity(currentUser, updateRequest);
            UserEntity updatedUser = userService.updateUser(currentUser.getId(), currentUser);
            UserResponse userResponse = userMapper.toUserResponse(updatedUser);
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
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Requête de suppression de l'utilisateur avec l'ID : {}", id);
        try {
            userService.deleteUser(id);
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
    @GetMapping("/download-pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadUsersPDF() {
        log.info("Requête de téléchargement du PDF des utilisateurs");
        try {
            List<UserEntity> users = userService.getAllUsers();
            byte[] pdfBytes = pdfService.generateUsersPDF(users);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "users-list.pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF des utilisateurs", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}