package com.example.userservice.service;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.List;

class KeycloakUserServiceTest {
    @InjectMocks
    private KeycloakUserService keycloakUserService;
    @Mock
    private Authentication authentication;
    @Mock
    private Jwt jwt;

    public KeycloakUserServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldDoSomething() {
        assertNotNull(keycloakUserService);
    }

    @Test
    void testIsCurrentUserAdmin_false() {
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsMap("realm_access")).thenReturn(Collections.singletonMap("roles", List.of("USER")));
        assertFalse(keycloakUserService.isCurrentUserAdmin(authentication));
    }
} 