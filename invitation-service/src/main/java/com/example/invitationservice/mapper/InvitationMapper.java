package com.example.invitationservice.mapper;

import com.example.invitationservice.entity.Invitation;
import com.example.invitationservice.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper pour convertir entre les entités Invitation et les DTOs
 * Centralise toute la logique de conversion
 */
@Component
public class InvitationMapper {

    /**
     * Convertit une Invitation en InvitationResponse
     */
    public InvitationResponse toInvitationResponse(Invitation entity) {
        if (entity == null) {
            return null;
        }
        
        return new InvitationResponse(
            entity.getId(),
            entity.getEventId(),
            entity.getUserId(),
            entity.getStatus().name(),
            entity.getInvitedAt(),
            entity.getRespondedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Convertit une Invitation en InvitationDTO
     */
    public InvitationDTO toInvitationDTO(Invitation entity) {
        if (entity == null) {
            return null;
        }
        
        return new InvitationDTO(
            entity.getId(),
            entity.getEventId(),
            entity.getUserId(),
            entity.getStatus().name(),
            entity.getInvitedAt(),
            entity.getRespondedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Convertit un InvitationRequest en Invitation (pour création)
     */
    public Invitation toInvitation(InvitationRequest request) {
        if (request == null) {
            return null;
        }
        
        return new Invitation(request.getEventId(), request.getUserId());
    }

    /**
     * Met à jour une Invitation avec les données d'un InvitationUpdateRequest
     */
    public void updateInvitation(Invitation entity, InvitationUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        
        if (request.getStatus() != null) {
            try {
                Invitation.InvitationStatus status = Invitation.InvitationStatus.valueOf(request.getStatus().toUpperCase());
                entity.setStatus(status);
                entity.setRespondedAt(LocalDateTime.now());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Statut d'invitation invalide: " + request.getStatus());
            }
        }
        
        entity.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Convertit une liste d'entités en liste de InvitationResponse
     */
    public List<InvitationResponse> toInvitationResponseList(List<Invitation> entities) {
        if (entities == null) {
            return null;
        }
        
        return entities.stream()
                .map(this::toInvitationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une liste d'entités en liste de InvitationDTO
     */
    public List<InvitationDTO> toInvitationDTOList(List<Invitation> entities) {
        if (entities == null) {
            return null;
        }
        
        return entities.stream()
                .map(this::toInvitationDTO)
                .collect(Collectors.toList());
    }
}
