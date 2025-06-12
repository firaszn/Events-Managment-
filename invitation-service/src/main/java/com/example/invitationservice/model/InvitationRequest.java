package com.example.invitationservice.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les requêtes de création d'invitations
 * Contient uniquement les champs nécessaires pour créer une invitation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationRequest {

    @NotNull(message = "L'ID de l'événement est obligatoire")
    private Long eventId;

    @NotNull(message = "L'ID de l'utilisateur est obligatoire")
    private Long userId;
}
