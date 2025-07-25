package com.example.userservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class KeycloakAuthControllerTest {
    @Autowired
    private KeycloakAuthController keycloakAuthController;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertThat(keycloakAuthController).isNotNull();
    }

    @Test
    void shouldReturnInfo() throws Exception {
        mockMvc.perform(get("/auth/keycloak/info"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Endpoints Keycloak disponibles")));
    }
} 