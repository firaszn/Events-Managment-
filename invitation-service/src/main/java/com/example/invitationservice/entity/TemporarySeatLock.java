package com.example.invitationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporarySeatLock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;
    private Integer row;
    private Integer number;
    private String userEmail;
    private LocalDateTime lockTime;
    private LocalDateTime expiryTime;

    @Version
    private Long version; // Pour la gestion optimiste des verrous

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
} 