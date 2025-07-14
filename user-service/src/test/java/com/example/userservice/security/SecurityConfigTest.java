package com.example.userservice.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {
    @Test
    void testSecurityConfigNotNull() {
        SecurityConfig config = new SecurityConfig();
        assertNotNull(config);
    }
} 