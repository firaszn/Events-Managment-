package com.example.eventservice.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les requêtes de création d'événements
 * Contient uniquement les champs modifiables par le client
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200, message = "Le titre ne peut pas dépasser 200 caractères")
    private String title;

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    private String description;

    @NotBlank(message = "Le lieu est obligatoire")
    @Size(max = 300, message = "Le lieu ne peut pas dépasser 300 caractères")
    private String location;

    @NotNull(message = "La date et l'heure sont obligatoires")
    private LocalDateTime dateTime;

    // L'organisateur est toujours l'admin, pas besoin de le spécifier dans la requête
}
