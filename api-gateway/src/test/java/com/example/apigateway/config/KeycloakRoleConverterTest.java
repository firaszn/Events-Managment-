package com.example.apigateway.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleConverterTest {
    @Test
    void canInstantiate() {
        KeycloakRoleConverter converter = new KeycloakRoleConverter();
        assertThat(converter).isNotNull();
    }
} 