package com.example.eventservice.model;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventRequest {
    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    private String description;

    @NotNull(message = "La date de l'événement est obligatoire")
    @Future(message = "La date de l'événement doit être dans le futur")
    private LocalDateTime eventDate;

    @NotBlank(message = "Le lieu est obligatoire")
    private String location;

    private Integer maxCapacity;

    private Boolean waitlistEnabled;
} 