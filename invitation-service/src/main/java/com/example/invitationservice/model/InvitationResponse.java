package com.example.invitationservice.model;

import com.example.invitationservice.entity.SeatInfo;
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
    private SeatInfo seatInfo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 