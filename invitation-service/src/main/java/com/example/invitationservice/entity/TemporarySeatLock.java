package com.example.invitationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "temporary_seat_lock", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"event_id", "row", "number"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporarySeatLock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "row", nullable = false)
    private Integer row;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "lock_time", nullable = false)
    private LocalDateTime lockTime;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @Version
    private Long version;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
} 