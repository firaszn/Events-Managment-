package com.example.userservice.service;

import com.example.userservice.entity.UserEntity;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindByEmail_found() {
        UserEntity user = new UserEntity();
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        UserEntity result = userService.findByEmail("test@example.com");
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testFindByEmail_notFound() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> userService.findByEmail("notfound@example.com"));
    }
} 