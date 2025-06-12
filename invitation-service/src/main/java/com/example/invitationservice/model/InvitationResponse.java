package com.example.invitationservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les réponses contenant les informations d'invitation
 * Inclut tous les champs y compris les métadonnées
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {

    private Long id;
    private Long eventId;
    private Long userId;
    private String status;
    private LocalDateTime invitedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructeur pour la création rapide sans dates de réponse
    public InvitationResponse(Long id, Long eventId, Long userId, String status, 
                             LocalDateTime invitedAt) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.status = status;
        this.invitedAt = invitedAt;
    }
}
