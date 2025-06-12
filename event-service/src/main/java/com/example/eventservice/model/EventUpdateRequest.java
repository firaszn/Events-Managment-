package com.example.eventservice.model;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les requêtes de mise à jour d'événements
 * Tous les champs sont optionnels pour permettre les mises à jour partielles
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventUpdateRequest {

    @Size(max = 200, message = "Le titre ne peut pas dépasser 200 caractères")
    private String title;

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    private String description;

    @Size(max = 300, message = "Le lieu ne peut pas dépasser 300 caractères")
    private String location;

    private LocalDateTime dateTime;

    private Long organizerId;
}
