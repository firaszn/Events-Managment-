package com.example.userservice.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {
    @Test
    void testUserEntityBuilder() {
        UserEntity user = UserEntity.builder().email("test@example.com").build();
        assertEquals("test@example.com", user.getEmail());
    }
} 