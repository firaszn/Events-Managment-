package com.example.invitationservice.service;

import com.example.invitationservice.repository.InvitationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Collections;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import com.example.invitationservice.entity.SeatInfo;


class InvitationServiceTest {
    @Mock
    private InvitationRepository invitationRepository;
    @InjectMocks
    private InvitationService invitationService;

    public InvitationServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnEmptyList() {
        when(invitationRepository.findAll()).thenReturn(Collections.emptyList());
        assertThat(invitationService.getAllInvitations()).isEmpty();
    }

    @Test
    void testIsUserRegisteredForEvent() {
        when(invitationRepository.existsByEventIdAndUserEmail(1L, "a@b.com")).thenReturn(true);
        assertThat(invitationService.isUserRegisteredForEvent(1L, "a@b.com")).isTrue();
    }

    @Test
    void testIsSeatOccupied() {
        SeatInfo seat = new SeatInfo();
        when(invitationRepository.existsByEventIdAndSeatInfo(1L, seat)).thenReturn(false);
        assertThat(invitationService.isSeatOccupied(1L, seat)).isFalse();
    }
} 