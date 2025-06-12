package com.example.eventservice.mapper;

import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper pour convertir entre les entités Event et les DTOs
 * Centralise toute la logique de conversion
 */
@Component
public class EventMapper {

    /**
     * Convertit une EventEntity en EventResponse
     */
    public EventResponse toEventResponse(EventEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new EventResponse(
            entity.getId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getLocation(),
            entity.getDateTime(),
            entity.getOrganizerId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Convertit une EventEntity en EventDTO
     */
    public EventDTO toEventDTO(EventEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new EventDTO(
            entity.getId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getLocation(),
            entity.getDateTime(),
            entity.getOrganizerId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Convertit un EventRequest en EventEntity (pour création)
     */
    public EventEntity toEventEntity(EventRequest request) {
        if (request == null) {
            return null;
        }
        
        EventEntity entity = new EventEntity();
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setLocation(request.getLocation());
        entity.setDateTime(request.getDateTime());
        entity.setOrganizerId(request.getOrganizerId());
        
        return entity;
    }

    /**
     * Met à jour une EventEntity avec les données d'un EventUpdateRequest
     */
    public void updateEventEntity(EventEntity entity, EventUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        
        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            entity.setLocation(request.getLocation());
        }
        if (request.getDateTime() != null) {
            entity.setDateTime(request.getDateTime());
        }
        if (request.getOrganizerId() != null) {
            entity.setOrganizerId(request.getOrganizerId());
        }
        
        entity.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Convertit une liste d'entités en liste de EventResponse
     */
    public List<EventResponse> toEventResponseList(List<EventEntity> entities) {
        if (entities == null) {
            return null;
        }
        
        return entities.stream()
                .map(this::toEventResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une liste d'entités en liste de EventDTO
     */
    public List<EventDTO> toEventDTOList(List<EventEntity> entities) {
        if (entities == null) {
            return null;
        }
        
        return entities.stream()
                .map(this::toEventDTO)
                .collect(Collectors.toList());
    }
}
