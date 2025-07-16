package com.example.userservice.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {
    @Test
    void canInstantiate() {
        UserMapper mapper = new UserMapper();
        assertThat(mapper).isNotNull();
    }
} 