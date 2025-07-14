package com.example.notificationservice.service;

import com.example.notificationservice.model.InvitationNotificationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class NotificationServiceTest {
    @Mock
    private EmailService emailService;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testHandleInvitationResponse_validJson() throws Exception {
        InvitationNotificationDTO dto = new InvitationNotificationDTO();
        dto.setUserEmail("test@example.com");
        dto.setEventTitle("Event");
        when(objectMapper.readValue(anyString(), eq(InvitationNotificationDTO.class))).thenReturn(dto);
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());
        notificationService.handleInvitationResponse("{\"userEmail\":\"test@example.com\"}");
        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());
    }
} 