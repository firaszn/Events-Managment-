package com.example.userservice.service;

import com.example.userservice.entity.UserEntity;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Optional;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserSyncServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserSyncService userSyncService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSyncUserFromKeycloak_UserNotFound() {
        when(keycloakService.getUserByEmail(anyString())).thenReturn(null);
        assertThrows(UserSyncService.UserSyncException.class, () -> userSyncService.syncUserFromKeycloak("test@example.com"));
    }

    @Test
    void testEnsureUserSyncOnLogin_UserExists() {
        UserEntity user = new UserEntity();
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        UserEntity result = userSyncService.ensureUserSyncOnLogin("test@example.com");
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testEnsureUserSyncOnLogin_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(keycloakService.getUserByEmail(anyString())).thenReturn(null);
        UserEntity result = userSyncService.ensureUserSyncOnLogin("notfound@example.com");
        assertNull(result);
    }
} 