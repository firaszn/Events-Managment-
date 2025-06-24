package com.example.invitationservice.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationRequest {
    
    @NotNull(message = "L'ID de l'événement est requis")
    private Long eventId;

    @NotNull(message = "Le titre de l'événement est requis")
    private String eventTitle;

    @NotNull(message = "L'email de l'utilisateur est requis")
    @Email(message = "L'email doit être valide")
    private String userEmail;
} 