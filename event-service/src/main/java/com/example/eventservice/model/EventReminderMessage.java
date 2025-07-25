package com.example.eventservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventReminderMessage {
    private Long eventId;
    private String eventTitle;
    private String eventDescription;
    private LocalDateTime eventDateTime;
    private String eventLocation;
    private String organizerId;
    private List<String> participantEmails;
} 