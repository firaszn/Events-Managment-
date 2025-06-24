package com.example.invitationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private String userEmail;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 