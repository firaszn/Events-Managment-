package com.example.userservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class KeycloakAuthControllerTest {
    @Autowired
    private KeycloakAuthController keycloakAuthController;

    @Test
    void contextLoads() {
        assertThat(keycloakAuthController).isNotNull();
    }
} 