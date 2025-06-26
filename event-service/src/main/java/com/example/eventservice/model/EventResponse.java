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
} 