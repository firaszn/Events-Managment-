package com.example.invitationservice.controller;

import com.example.invitationservice.service.InvitationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.kafka.core.KafkaTemplate;
import com.example.invitationservice.service.SeatLockService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import java.util.List;
import java.util.Map;

@WebMvcTest(InvitationController.class)
class InvitationControllerTest {
    @TestConfiguration
    static class MockConfig {
        @Bean
        InvitationService invitationService() {
            return Mockito.mock(InvitationService.class);
        }
        @Bean
        SeatLockService seatLockService() {
            return Mockito.mock(SeatLockService.class);
        }
        @Bean
        KafkaTemplate<String, String> kafkaTemplate() {
            return Mockito.mock(KafkaTemplate.class);
        }
    }
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private InvitationService invitationService;
    @Autowired
    private SeatLockService seatLockService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void shouldReturnOkForGetAll() throws Exception {
        when(invitationService.getAllInvitations()).thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(get("/invitations")
                .with(jwt().jwt(jwt -> jwt
                    .claim("email", "test@example.com")
                    .claim("realm_access", Map.of("roles", List.of("USER")))
                    .subject("user-id-123")
                )))
            .andExpect(status().isOk());
    }
} 