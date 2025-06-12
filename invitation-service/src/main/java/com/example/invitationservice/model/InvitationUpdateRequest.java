package com.example.invitationservice.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les requêtes de mise à jour d'invitations
 * Utilisé principalement pour répondre aux invitations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationUpdateRequest {

    @NotNull(message = "Le statut est obligatoire")
    private String status; // ACCEPTED, DECLINED
}
