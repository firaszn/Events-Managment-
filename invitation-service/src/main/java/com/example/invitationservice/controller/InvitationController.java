package com.example.invitationservice.controller;

import com.example.invitationservice.entity.InvitationEntity;
import com.example.invitationservice.model.InvitationRequest;
import com.example.invitationservice.model.InvitationResponse;
import com.example.invitationservice.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    public ResponseEntity<InvitationResponse> createInvitation(@Valid @RequestBody InvitationRequest request) {
        InvitationEntity invitation = invitationService.createInvitation(request);
        return new ResponseEntity<>(toResponse(invitation), HttpStatus.CREATED);
    }

    private InvitationResponse toResponse(InvitationEntity entity) {
        return InvitationResponse.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .eventTitle(entity.getEventTitle())
                .userEmail(entity.getUserEmail())
                .status(entity.getStatus().name())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
} 