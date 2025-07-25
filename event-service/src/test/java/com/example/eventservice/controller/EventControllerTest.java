package com.example.eventservice.controller;

import com.example.eventservice.service.EventService;
import com.example.eventservice.mapper.EventMapper;
import com.example.eventservice.client.InvitationClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    value = EventController.class,
    excludeAutoConfiguration = {
        org.springframework.cloud.openfeign.FeignAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = InvitationClient.class)
)
@Import(EventControllerTest.MockConfig.class)
class EventControllerTest {

    @TestConfiguration
    static class MockConfig {
        @Bean
        EventService eventService() {
            return Mockito.mock(EventService.class);
        }
        @Bean
        EventMapper eventMapper() {
            return Mockito.mock(EventMapper.class);
        }
        @Bean
        InvitationClient invitationClient() {
            return Mockito.mock(InvitationClient.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventService eventService;
    @Autowired
    private EventMapper eventMapper;
    @Autowired
    private InvitationClient invitationClient;


    @Test
    void shouldReturnOkForGetAll() throws Exception {
        when(eventService.getAllEvents()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/events")
                        .with(jwt().jwt(jwt -> jwt
                                .claim("email", "test@example.com")
                                .subject("user-id-123")
                        )))
                .andExpect(status().isOk());
    }
}