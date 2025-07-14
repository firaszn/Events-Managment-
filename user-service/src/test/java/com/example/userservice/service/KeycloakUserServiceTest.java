package com.example.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakUserServiceTest {
    @InjectMocks
    private KeycloakUserService keycloakUserService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testKeycloakUserServiceNotNull() {
        assertNotNull(keycloakUserService);
    }
} 