package com.example.eventservice.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private String location;
    private String organizerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean userRegistered;
    private Integer maxCapacity;
    private Boolean waitlistEnabled;
    private Long confirmedParticipants;
    private Long waitlistCount;
    private Integer userWaitlistPosition;
    private String userWaitlistStatus; // WAITING, NOTIFIED, CONFIRMED, EXPIRED, CANCELLED
    private Boolean userHasPendingInvitation; // L'utilisateur a une invitation en attente
    private String userStatus; // Statut personnalisé pour l'utilisateur (ex: CANCELLED)
}