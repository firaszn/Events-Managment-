package com.example.invitationservice.controller;

import com.example.invitationservice.entity.Invitation;
import com.example.invitationservice.entity.Invitation.InvitationStatus;
import com.example.invitationservice.mapper.InvitationMapper;
import com.example.invitationservice.model.*;
import com.example.invitationservice.service.InvitationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST pour la gestion des invitations
 * 
 * Endpoints :
 * - POST /invitations – Inviter un participant
 * - PUT /invitations/{id}/respond – Répondre (accepter/refuser)
 * - GET /invitations/user/{userId} – Voir les invitations d'un utilisateur
 */
@RestController
@RequestMapping("/api/invitations")
@CrossOrigin(origins = "*")
public class InvitationController {

    private static final Logger logger = LoggerFactory.getLogger(InvitationController.class);

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private InvitationMapper invitationMapper;

    /**
     * POST /invitations – Créer une invitation
     */
    @PostMapping
    public ResponseEntity<InvitationResponse> createInvitation(@Valid @RequestBody InvitationRequest invitationRequest) {
        logger.info("Requête de création d'invitation reçue pour l'événement {} et l'utilisateur {}",
                   invitationRequest.getEventId(), invitationRequest.getUserId());

        try {
            Invitation invitation = invitationMapper.toInvitation(invitationRequest);
            Invitation createdInvitation = invitationService.createInvitation(invitation);
            InvitationResponse response = invitationMapper.toInvitationResponse(createdInvitation);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            logger.error("Erreur lors de la création de l'invitation : {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (Exception e) {
            logger.error("Erreur lors de la création de l'invitation", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * POST /invitations/simple – Créer une invitation simple avec eventId et userId
     */
    @PostMapping("/simple")
    public ResponseEntity<InvitationResponse> createSimpleInvitation(@RequestBody InvitationRequest request) {
        logger.info("Requête de création d'invitation simple reçue");

        try {
            if (request.getEventId() == null || request.getUserId() == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            Invitation createdInvitation = invitationService.createInvitation(request.getEventId(), request.getUserId());
            InvitationResponse response = invitationMapper.toInvitationResponse(createdInvitation);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            logger.error("Erreur lors de la création de l'invitation : {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (Exception e) {
            logger.error("Erreur lors de la création de l'invitation", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /invitations/{id}/respond – Répondre à une invitation
     */
    @PutMapping("/{id}/respond")
    public ResponseEntity<Invitation> respondToInvitation(@PathVariable Long id, 
                                                         @RequestBody Map<String, String> response) {
        logger.info("Requête de réponse à l'invitation {} reçue", id);
        
        try {
            String statusStr = response.get("status");
            if (statusStr == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            InvitationStatus status;
            try {
                status = InvitationStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.error("Statut d'invitation invalide : {}", statusStr);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            Invitation updatedInvitation = invitationService.respondToInvitation(id, status);
            return new ResponseEntity<>(updatedInvitation, HttpStatus.OK);
        } catch (RuntimeException e) {
            logger.error("Erreur lors de la réponse à l'invitation : {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Erreur lors de la réponse à l'invitation", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /invitations/{id}/accept – Accepter une invitation
     */
    @PutMapping("/{id}/accept")
    public ResponseEntity<InvitationResponse> acceptInvitation(@PathVariable Long id) {
        logger.info("Requête d'acceptation de l'invitation {} reçue", id);

        try {
            Invitation updatedInvitation = invitationService.acceptInvitation(id);
            InvitationResponse response = invitationMapper.toInvitationResponse(updatedInvitation);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            logger.error("Erreur lors de l'acceptation de l'invitation : {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Erreur lors de l'acceptation de l'invitation", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /invitations/{id}/decline – Refuser une invitation
     */
    @PutMapping("/{id}/decline")
    public ResponseEntity<InvitationResponse> declineInvitation(@PathVariable Long id) {
        logger.info("Requête de refus de l'invitation {} reçue", id);

        try {
            Invitation updatedInvitation = invitationService.declineInvitation(id);
            InvitationResponse response = invitationMapper.toInvitationResponse(updatedInvitation);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            logger.error("Erreur lors du refus de l'invitation : {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Erreur lors du refus de l'invitation", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /invitations/{id} – Obtenir une invitation par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvitationResponse> getInvitationById(@PathVariable Long id) {
        logger.info("Requête de récupération d'invitation reçue pour l'ID : {}", id);

        try {
            Invitation invitation = invitationService.getInvitationById(id);
            InvitationResponse response = invitationMapper.toInvitationResponse(invitation);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            logger.error("Invitation non trouvée avec l'ID : {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'invitation", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /invitations/user/{userId} – Obtenir toutes les invitations d'un utilisateur
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InvitationResponse>> getInvitationsByUser(@PathVariable Long userId,
                                                                @RequestParam(required = false) String status) {
        logger.info("Requête de récupération des invitations pour l'utilisateur : {} avec statut : {}", userId, status);

        try {
            List<Invitation> invitations;

            if (status != null) {
                switch (status.toLowerCase()) {
                    case "pending":
                        invitations = invitationService.getPendingInvitationsByUser(userId);
                        break;
                    case "accepted":
                        invitations = invitationService.getAcceptedInvitationsByUser(userId);
                        break;
                    case "declined":
                        invitations = invitationService.getDeclinedInvitationsByUser(userId);
                        break;
                    default:
                        invitations = invitationService.getInvitationsByUser(userId);
                        break;
                }
            } else {
                invitations = invitationService.getInvitationsByUser(userId);
            }

            List<InvitationResponse> response = invitationMapper.toInvitationResponseList(invitations);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des invitations de l'utilisateur", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /invitations/event/{eventId} – Obtenir toutes les invitations pour un événement
     */
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<InvitationResponse>> getInvitationsByEvent(@PathVariable Long eventId) {
        logger.info("Requête de récupération des invitations pour l'événement : {}", eventId);

        try {
            List<Invitation> invitations = invitationService.getInvitationsByEvent(eventId);
            List<InvitationResponse> response = invitationMapper.toInvitationResponseList(invitations);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des invitations de l'événement", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /invitations/event/{eventId}/stats – Obtenir les statistiques d'invitations pour un événement
     */
    @GetMapping("/event/{eventId}/stats")
    public ResponseEntity<InvitationService.InvitationStats> getInvitationStats(@PathVariable Long eventId) {
        logger.info("Requête de récupération des statistiques d'invitations pour l'événement : {}", eventId);
        
        try {
            InvitationService.InvitationStats stats = invitationService.getInvitationStats(eventId);
            return new ResponseEntity<>(stats, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des statistiques d'invitations", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DELETE /invitations/{id} – Supprimer une invitation
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvitation(@PathVariable Long id) {
        logger.info("Requête de suppression d'invitation reçue pour l'ID : {}", id);
        
        try {
            invitationService.deleteInvitation(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            logger.error("Invitation non trouvée avec l'ID : {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de l'invitation", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DELETE /invitations/event/{eventId} – Supprimer toutes les invitations pour un événement
     */
    @DeleteMapping("/event/{eventId}")
    public ResponseEntity<Void> deleteInvitationsByEvent(@PathVariable Long eventId) {
        logger.info("Requête de suppression des invitations pour l'événement : {}", eventId);
        
        try {
            invitationService.deleteInvitationsByEvent(eventId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression des invitations de l'événement", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
