package com.example.invitationservice.repository;

import com.example.invitationservice.entity.Invitation;
import com.example.invitationservice.entity.Invitation.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des invitations
 */
@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    /**
     * Trouve toutes les invitations d'un utilisateur
     * @param userId ID de l'utilisateur
     * @return Liste des invitations
     */
    List<Invitation> findByUserId(Long userId);

    /**
     * Trouve toutes les invitations d'un utilisateur triées par date d'invitation
     * @param userId ID de l'utilisateur
     * @return Liste des invitations triées
     */
    List<Invitation> findByUserIdOrderByInvitedAtDesc(Long userId);

    /**
     * Trouve toutes les invitations pour un événement
     * @param eventId ID de l'événement
     * @return Liste des invitations
     */
    List<Invitation> findByEventId(Long eventId);

    /**
     * Trouve toutes les invitations pour un événement triées par statut
     * @param eventId ID de l'événement
     * @return Liste des invitations triées
     */
    List<Invitation> findByEventIdOrderByStatusAscInvitedAtDesc(Long eventId);

    /**
     * Trouve les invitations d'un utilisateur avec un statut spécifique
     * @param userId ID de l'utilisateur
     * @param status Statut de l'invitation
     * @return Liste des invitations
     */
    List<Invitation> findByUserIdAndStatus(Long userId, InvitationStatus status);

    /**
     * Trouve les invitations d'un événement avec un statut spécifique
     * @param eventId ID de l'événement
     * @param status Statut de l'invitation
     * @return Liste des invitations
     */
    List<Invitation> findByEventIdAndStatus(Long eventId, InvitationStatus status);

    /**
     * Trouve une invitation spécifique pour un utilisateur et un événement
     * @param eventId ID de l'événement
     * @param userId ID de l'utilisateur
     * @return Invitation optionnelle
     */
    Optional<Invitation> findByEventIdAndUserId(Long eventId, Long userId);

    /**
     * Vérifie si une invitation existe déjà pour un utilisateur et un événement
     * @param eventId ID de l'événement
     * @param userId ID de l'utilisateur
     * @return true si l'invitation existe
     */
    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    /**
     * Trouve les invitations en attente d'un utilisateur
     * @param userId ID de l'utilisateur
     * @return Liste des invitations en attente
     */
    @Query("SELECT i FROM Invitation i WHERE i.userId = :userId AND i.status = 'PENDING' ORDER BY i.invitedAt DESC")
    List<Invitation> findPendingInvitationsByUser(@Param("userId") Long userId);

    /**
     * Trouve les invitations acceptées d'un utilisateur
     * @param userId ID de l'utilisateur
     * @return Liste des invitations acceptées
     */
    @Query("SELECT i FROM Invitation i WHERE i.userId = :userId AND i.status = 'ACCEPTED' ORDER BY i.respondedAt DESC")
    List<Invitation> findAcceptedInvitationsByUser(@Param("userId") Long userId);

    /**
     * Trouve les invitations refusées d'un utilisateur
     * @param userId ID de l'utilisateur
     * @return Liste des invitations refusées
     */
    @Query("SELECT i FROM Invitation i WHERE i.userId = :userId AND i.status = 'DECLINED' ORDER BY i.respondedAt DESC")
    List<Invitation> findDeclinedInvitationsByUser(@Param("userId") Long userId);

    /**
     * Compte le nombre d'invitations par statut pour un événement
     * @param eventId ID de l'événement
     * @param status Statut de l'invitation
     * @return Nombre d'invitations
     */
    long countByEventIdAndStatus(Long eventId, InvitationStatus status);

    /**
     * Compte le nombre total d'invitations pour un événement
     * @param eventId ID de l'événement
     * @return Nombre total d'invitations
     */
    long countByEventId(Long eventId);

    /**
     * Trouve les invitations récentes (dernières 24h)
     * @param since Date depuis laquelle chercher
     * @return Liste des invitations récentes
     */
    @Query("SELECT i FROM Invitation i WHERE i.invitedAt >= :since ORDER BY i.invitedAt DESC")
    List<Invitation> findRecentInvitations(@Param("since") LocalDateTime since);

    /**
     * Trouve les réponses récentes aux invitations (dernières 24h)
     * @param since Date depuis laquelle chercher
     * @return Liste des réponses récentes
     */
    @Query("SELECT i FROM Invitation i WHERE i.respondedAt >= :since AND i.status != 'PENDING' ORDER BY i.respondedAt DESC")
    List<Invitation> findRecentResponses(@Param("since") LocalDateTime since);

    /**
     * Trouve les invitations expirées (en attente depuis plus de X jours)
     * @param expiredBefore Date avant laquelle les invitations sont considérées comme expirées
     * @return Liste des invitations expirées
     */
    @Query("SELECT i FROM Invitation i WHERE i.status = 'PENDING' AND i.invitedAt < :expiredBefore ORDER BY i.invitedAt ASC")
    List<Invitation> findExpiredPendingInvitations(@Param("expiredBefore") LocalDateTime expiredBefore);

    /**
     * Supprime toutes les invitations pour un événement
     * @param eventId ID de l'événement
     */
    void deleteByEventId(Long eventId);

    /**
     * Supprime toutes les invitations d'un utilisateur
     * @param userId ID de l'utilisateur
     */
    void deleteByUserId(Long userId);
}
