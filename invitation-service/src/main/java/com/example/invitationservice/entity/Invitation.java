package com.example.invitationservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Entité représentant une invitation à un événement
 * Champs : id, eventId, userId, status (PENDING, ACCEPTED, DECLINED)
 */
@Entity
@Table(name = "invitations")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "L'ID de l'événement est obligatoire")
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @NotNull(message = "L'ID de l'utilisateur est obligatoire")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enum pour le statut de l'invitation
    public enum InvitationStatus {
        PENDING("En attente"),
        ACCEPTED("Acceptée"),
        DECLINED("Refusée");

        private final String displayName;

        InvitationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Constructeurs
    public Invitation() {
    }

    public Invitation(Long eventId, Long userId) {
        this.eventId = eventId;
        this.userId = userId;
        this.status = InvitationStatus.PENDING;
        this.invitedAt = LocalDateTime.now();
    }

    public Invitation(Long eventId, Long userId, InvitationStatus status) {
        this.eventId = eventId;
        this.userId = userId;
        this.status = status;
        this.invitedAt = LocalDateTime.now();
    }

    // Méthodes de cycle de vie JPA
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (invitedAt == null) {
            invitedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Méthodes métier
    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void decline() {
        this.status = InvitationStatus.DECLINED;
        this.respondedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return this.status == InvitationStatus.PENDING;
    }

    public boolean isAccepted() {
        return this.status == InvitationStatus.ACCEPTED;
    }

    public boolean isDeclined() {
        return this.status == InvitationStatus.DECLINED;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
        if (status != InvitationStatus.PENDING && respondedAt == null) {
            respondedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(LocalDateTime invitedAt) {
        this.invitedAt = invitedAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Invitation{" +
                "id=" + id +
                ", eventId=" + eventId +
                ", userId=" + userId +
                ", status=" + status +
                ", invitedAt=" + invitedAt +
                ", respondedAt=" + respondedAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
