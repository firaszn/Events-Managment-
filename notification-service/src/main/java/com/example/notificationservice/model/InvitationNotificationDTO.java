package com.example.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationNotificationDTO {
    private String eventId;
    private String userEmail;
    private String eventTitle;
    private SeatInfoDTO seatInfo;
}

