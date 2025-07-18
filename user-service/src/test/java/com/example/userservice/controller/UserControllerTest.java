package com.example.userservice.controller;

import com.example.userservice.model.UserResponse;
import com.example.userservice.service.KeycloakUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Mock
    private KeycloakUserService keycloakUserService;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllUsers_forbidden() {
        when(keycloakUserService.isCurrentUserAdmin(authentication)).thenReturn(false);
        ResponseEntity<List<UserResponse>> response = userController.getAllUsers(authentication);
        int status = response.getStatusCode().value();
        assertEquals(403, status);
    }
} 