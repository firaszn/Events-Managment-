package com.example.invitationservice.service;

import com.example.invitationservice.entity.InvitationEntity;
import com.example.invitationservice.repository.InvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvitationServiceTest {
    @Mock
    private InvitationRepository invitationRepository;
    @InjectMocks
    private InvitationService invitationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllInvitations_empty() {
        when(invitationRepository.findAll()).thenReturn(Collections.emptyList());
        List<InvitationEntity> result = invitationService.getAllInvitations();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
} 