package com.example.userservice.mapper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {
    @Test
    void testUserMapperNotNull() {
        UserMapper mapper = new UserMapper();
        assertNotNull(mapper);
    }
} 