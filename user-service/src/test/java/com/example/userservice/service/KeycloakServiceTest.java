package com.example.userservice.service;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.mockito.Mock;

import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import javax.ws.rs.core.Response;

class KeycloakServiceTest {
    @InjectMocks
    private KeycloakService keycloakService;
    @Mock
    private Keycloak keycloak;
    @Mock
    private RealmResource realmResource;
    @Mock
    private UsersResource usersResource;
    @Mock
    private Response response;

    public KeycloakServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldDoSomething() {
        assertNotNull(keycloakService);
    }

    @Test
    void testGetUserByEmail_returnsNullOnException() {
        ReflectionTestUtils.setField(keycloakService, "serverUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(keycloakService, "realm", "realm");
        when(keycloak.realm(anyString())).thenThrow(new RuntimeException("fail"));
        assertNull(keycloakService.getUserByEmail("test@example.com"));
    }

    @Test
    void testValidateUserCredentials_returnsFalseOnException() {
        ReflectionTestUtils.setField(keycloakService, "serverUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(keycloakService, "realm", "realm");
        ReflectionTestUtils.setField(keycloakService, "clientSecret", "secret");
        assertFalse(keycloakService.validateUserCredentials("email", "pass"));
    }
} 