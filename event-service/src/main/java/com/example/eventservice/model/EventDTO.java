package com.example.eventservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO général pour les événements
 * Utilisé pour les transferts de données internes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDTO {

    private Long id;
    private String title;
    private String description;
    private String location;
    private LocalDateTime dateTime;
    private Long organizerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
