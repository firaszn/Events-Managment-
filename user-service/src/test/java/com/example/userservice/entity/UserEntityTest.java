package com.example.userservice.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityTest {
    @Test
    void canInstantiate() {
        UserEntity user = new UserEntity();
        assertThat(user).isNotNull();
    }
} 