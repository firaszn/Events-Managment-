package com.example.userservice.controller;

import com.example.userservice.service.KeycloakAuthService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KeycloakAuthControllerTest {
    @Test
    void testKeycloakAuthControllerNotNull() {
        KeycloakAuthService mockService = mock(KeycloakAuthService.class);
        KeycloakAuthController controller = new KeycloakAuthController(mockService);
        assertNotNull(controller);
    }
} 