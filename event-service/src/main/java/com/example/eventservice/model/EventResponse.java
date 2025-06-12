package com.example.eventservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les réponses contenant les informations d'événement
 * Inclut tous les champs y compris les métadonnées
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private String location;
    private LocalDateTime dateTime;
    private Long organizerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructeur pour la création rapide sans dates
    public EventResponse(Long id, String title, String description, String location, 
                        LocalDateTime dateTime, Long organizerId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.dateTime = dateTime;
        this.organizerId = organizerId;
    }
}
