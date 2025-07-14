package com.example.userservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class KeycloakRoleConverterTest {
    @Test
    void testConvertNotNull() {
        KeycloakRoleConverter converter = new KeycloakRoleConverter();
        Jwt jwt = new Jwt("token", null, null, Map.of("alg", "none"), Collections.emptyMap());
        assertNotNull(converter.convert(jwt));
    }
} 