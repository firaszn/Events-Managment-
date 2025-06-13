package com.example.userservice.mapper;

import com.example.userservice.entity.UserEntity;
import com.example.userservice.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper pour convertir entre les entités User et les DTOs
 * Centralise toute la logique de conversion
 */
@Component
public class UserMapper {

    /**
     * Convertit une UserEntity en UserResponse
     */
    public UserResponse toUserResponse(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new UserResponse(
            entity.getId(),
            entity.getUsernameField(),
            entity.getEmail(),
            entity.getFirstName(),
            entity.getLastName(),
            entity.getPhoneNumber(),
            entity.getRole().name(),
            entity.isEnabled(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Convertit une UserEntity en UserDTO
     */
    public UserDTO toUserDTO(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new UserDTO(
            entity.getId(),
            entity.getUsernameField(),
            entity.getEmail(),
            entity.getFirstName(),
            entity.getLastName(),
            entity.getPhoneNumber(),
            entity.getRole().name(),
            entity.isEnabled(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Convertit un UserRequest en UserEntity (pour création)
     */
    public UserEntity toUserEntity(UserRequest request) {
        if (request == null) {
            return null;
        }
        
        UserEntity entity = new UserEntity();
        entity.setUsernameField(request.getUsername());
        entity.setEmail(request.getEmail());
        entity.setPassword(request.getPassword()); // Sera hashé dans le service
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setRole(request.getRole() != null ?
            UserEntity.Role.valueOf(request.getRole().toUpperCase()) :
            UserEntity.Role.USER);
        entity.setEnabled(true);
        
        return entity;
    }

    /**
     * Met à jour une UserEntity avec les données d'un UserUpdateRequest
     */
    public void updateUserEntity(UserEntity entity, UserUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getUsername() != null) {
            entity.setUsernameField(request.getUsername());
        }
        if (request.getEmail() != null) {
            entity.setEmail(request.getEmail());
        }
        if (request.getPassword() != null) {
            entity.setPassword(request.getPassword()); // Sera encodé dans le service
        }

        if (request.getFirstName() != null) {
            entity.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            entity.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            entity.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRole() != null) {
            entity.setRole(UserEntity.Role.valueOf(request.getRole().toUpperCase()));
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }

        entity.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Convertit une liste d'entités en liste de UserResponse
     */
    public List<UserResponse> toUserResponseList(List<UserEntity> entities) {
        if (entities == null) {
            return null;
        }
        
        return entities.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une liste d'entités en liste de UserDTO
     */
    public List<UserDTO> toUserDTOList(List<UserEntity> entities) {
        if (entities == null) {
            return null;
        }
        
        return entities.stream()
                .map(this::toUserDTO)
                .collect(Collectors.toList());
    }
}
