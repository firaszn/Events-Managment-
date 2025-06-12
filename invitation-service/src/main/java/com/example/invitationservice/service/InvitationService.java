package com.example.invitationservice.service;

import com.example.invitationservice.entity.Invitation;
import com.example.invitationservice.entity.Invitation.InvitationStatus;
import com.example.invitationservice.repository.InvitationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service métier pour la gestion des invitations
 */
@Service
@Transactional
public class InvitationService {

    private static final Logger logger = LoggerFactory.getLogger(InvitationService.class);

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // Topic Kafka
    private static final String INVITATION_RESPONDED_TOPIC = "invitation.responded";

    /**
     * Créer une nouvelle invitation
     */
    public Invitation createInvitation(Long eventId, Long userId) {
        logger.info("Création d'une invitation pour l'événement {} et l'utilisateur {}", eventId, userId);
        
        // Vérifier si l'invitation existe déjà
        if (invitationRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new RuntimeException("Une invitation existe déjà pour cet utilisateur et cet événement");
        }
        
        Invitation invitation = new Invitation(eventId, userId);
        Invitation savedInvitation = invitationRepository.save(invitation);
        
        logger.info("Invitation créée avec succès avec l'ID : {}", savedInvitation.getId());
        return savedInvitation;
    }

    /**
     * Créer une invitation avec un objet Invitation complet
     */
    public Invitation createInvitation(Invitation invitation) {
        logger.info("Création d'une invitation pour l'événement {} et l'utilisateur {}", 
                   invitation.getEventId(), invitation.getUserId());
        
        // Vérifier si l'invitation existe déjà
        if (invitationRepository.existsByEventIdAndUserId(invitation.getEventId(), invitation.getUserId())) {
            throw new RuntimeException("Une invitation existe déjà pour cet utilisateur et cet événement");
        }
        
        Invitation savedInvitation = invitationRepository.save(invitation);
        
        logger.info("Invitation créée avec succès avec l'ID : {}", savedInvitation.getId());
        return savedInvitation;
    }

    /**
     * Répondre à une invitation (accepter ou refuser)
     */
    public Invitation respondToInvitation(Long invitationId, InvitationStatus response) {
        logger.info("Réponse à l'invitation {} avec le statut {}", invitationId, response);
        
        if (response == InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Le statut de réponse ne peut pas être PENDING");
        }
        
        Optional<Invitation> optionalInvitation = invitationRepository.findById(invitationId);
        if (optionalInvitation.isEmpty()) {
            throw new RuntimeException("Invitation non trouvée avec l'ID : " + invitationId);
        }
        
        Invitation invitation = optionalInvitation.get();
        
        if (!invitation.isPending()) {
            throw new RuntimeException("Cette invitation a déjà reçu une réponse");
        }
        
        // Mettre à jour le statut
        invitation.setStatus(response);
        invitation.setRespondedAt(LocalDateTime.now());
        
        Invitation updatedInvitation = invitationRepository.save(invitation);
        
        // Publier l'événement Kafka
        publishInvitationResponse(updatedInvitation);
        
        logger.info("Réponse à l'invitation enregistrée avec succès : {}", updatedInvitation.getId());
        return updatedInvitation;
    }

    /**
     * Accepter une invitation
     */
    public Invitation acceptInvitation(Long invitationId) {
        return respondToInvitation(invitationId, InvitationStatus.ACCEPTED);
    }

    /**
     * Refuser une invitation
     */
    public Invitation declineInvitation(Long invitationId) {
        return respondToInvitation(invitationId, InvitationStatus.DECLINED);
    }

    /**
     * Obtenir une invitation par ID
     */
    @Transactional(readOnly = true)
    public Invitation getInvitationById(Long id) {
        logger.info("Recherche de l'invitation avec l'ID : {}", id);
        
        return invitationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invitation non trouvée avec l'ID : " + id));
    }

    /**
     * Obtenir toutes les invitations d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Invitation> getInvitationsByUser(Long userId) {
        logger.info("Récupération des invitations pour l'utilisateur : {}", userId);
        return invitationRepository.findByUserIdOrderByInvitedAtDesc(userId);
    }

    /**
     * Obtenir toutes les invitations pour un événement
     */
    @Transactional(readOnly = true)
    public List<Invitation> getInvitationsByEvent(Long eventId) {
        logger.info("Récupération des invitations pour l'événement : {}", eventId);
        return invitationRepository.findByEventIdOrderByStatusAscInvitedAtDesc(eventId);
    }

    /**
     * Obtenir les invitations en attente d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Invitation> getPendingInvitationsByUser(Long userId) {
        logger.info("Récupération des invitations en attente pour l'utilisateur : {}", userId);
        return invitationRepository.findPendingInvitationsByUser(userId);
    }

    /**
     * Obtenir les invitations acceptées d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Invitation> getAcceptedInvitationsByUser(Long userId) {
        logger.info("Récupération des invitations acceptées pour l'utilisateur : {}", userId);
        return invitationRepository.findAcceptedInvitationsByUser(userId);
    }

    /**
     * Obtenir les invitations refusées d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Invitation> getDeclinedInvitationsByUser(Long userId) {
        logger.info("Récupération des invitations refusées pour l'utilisateur : {}", userId);
        return invitationRepository.findDeclinedInvitationsByUser(userId);
    }

    /**
     * Obtenir les statistiques d'invitations pour un événement
     */
    @Transactional(readOnly = true)
    public InvitationStats getInvitationStats(Long eventId) {
        logger.info("Récupération des statistiques d'invitations pour l'événement : {}", eventId);
        
        long total = invitationRepository.countByEventId(eventId);
        long pending = invitationRepository.countByEventIdAndStatus(eventId, InvitationStatus.PENDING);
        long accepted = invitationRepository.countByEventIdAndStatus(eventId, InvitationStatus.ACCEPTED);
        long declined = invitationRepository.countByEventIdAndStatus(eventId, InvitationStatus.DECLINED);
        
        return new InvitationStats(eventId, total, pending, accepted, declined);
    }

    /**
     * Supprimer une invitation
     */
    public void deleteInvitation(Long invitationId) {
        logger.info("Suppression de l'invitation avec l'ID : {}", invitationId);
        
        if (!invitationRepository.existsById(invitationId)) {
            throw new RuntimeException("Invitation non trouvée avec l'ID : " + invitationId);
        }
        
        invitationRepository.deleteById(invitationId);
        logger.info("Invitation supprimée avec succès : {}", invitationId);
    }

    /**
     * Supprimer toutes les invitations pour un événement
     */
    public void deleteInvitationsByEvent(Long eventId) {
        logger.info("Suppression de toutes les invitations pour l'événement : {}", eventId);
        invitationRepository.deleteByEventId(eventId);
        logger.info("Invitations supprimées pour l'événement : {}", eventId);
    }

    /**
     * Publier une réponse d'invitation sur Kafka
     */
    private void publishInvitationResponse(Invitation invitation) {
        try {
            kafkaTemplate.send(INVITATION_RESPONDED_TOPIC, invitation);
            logger.info("Réponse d'invitation publiée sur Kafka pour l'invitation : {}", invitation.getId());
        } catch (Exception e) {
            logger.error("Erreur lors de la publication de la réponse d'invitation sur Kafka", e);
        }
    }

    /**
     * Classe pour les statistiques d'invitations
     */
    public static class InvitationStats {
        private final Long eventId;
        private final long total;
        private final long pending;
        private final long accepted;
        private final long declined;

        public InvitationStats(Long eventId, long total, long pending, long accepted, long declined) {
            this.eventId = eventId;
            this.total = total;
            this.pending = pending;
            this.accepted = accepted;
            this.declined = declined;
        }

        // Getters
        public Long getEventId() { return eventId; }
        public long getTotal() { return total; }
        public long getPending() { return pending; }
        public long getAccepted() { return accepted; }
        public long getDeclined() { return declined; }
        
        public double getAcceptanceRate() {
            return total > 0 ? (double) accepted / total * 100 : 0;
        }
        
        public double getResponseRate() {
            return total > 0 ? (double) (accepted + declined) / total * 100 : 0;
        }
    }
}
