package com.example.userservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class KeycloakServiceTest {
    @Autowired
    private KeycloakService keycloakService;

    @Test
    void contextLoads() {
        assertThat(keycloakService).isNotNull();
    }
} 