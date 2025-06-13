package com.example.userservice.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les requêtes de mise à jour d'utilisateurs
 * Tous les champs sont optionnels pour permettre les mises à jour partielles
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit contenir entre 3 et 50 caractères")
    private String username;

    @Email(message = "L'email doit être valide")
    private String email;

    @Size(max = 50, message = "Le prénom ne peut pas dépasser 50 caractères")
    private String firstName;

    @Size(max = 50, message = "Le nom ne peut pas dépasser 50 caractères")
    private String lastName;

    @Size(max = 15, message = "Le numéro de téléphone ne peut pas dépasser 15 caractères")
    private String phoneNumber;

    private String role;

    private Boolean enabled;

    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;
}
