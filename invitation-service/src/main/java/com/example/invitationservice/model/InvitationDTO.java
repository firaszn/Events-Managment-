package com.example.invitationservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO général pour les invitations
 * Utilisé pour les transferts de données internes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationDTO {

    private Long id;
    private Long eventId;
    private Long userId;
    private String status;
    private LocalDateTime invitedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
